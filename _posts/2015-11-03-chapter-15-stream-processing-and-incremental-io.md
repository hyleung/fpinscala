---
layout: post
title:  "Chapter 15: Stream Processing and Incremental IO" 
date:   2015-11-03  20:32:40 -0700
categories: fpinscala chapter_notes
---

# Stream Processing and Incremental IO

So far we have seen types like `IO` and `ST` which work by embedding an imperative style in our functional Scala
program. What we lose, however, is the ability to use composition to build our programs.

## A motivating example: counting line numbers in a file

Check whether a file contains greater than 40,000 lines.

Using an imperative style, might look something like this:

{%highlight scala%}
def linesGt40k(fileName:String):IO[Boolean] = IO {
    val src = io.Source.fromFile(filename)
    try {
        var count = 0
        val lines = Iterator[String] = src.getLines
        while (count <= 40000 && lines.hasNext) {
            lines.next
            count += 1
        }
        count > 40000
   finally src.close
}
{%endhighlight%}

On the plus side, this code doesn't load the entire file into memory. It also terminates as soon as it has an answer.

But...
- we have to remember to close the file after we're done. What we *want* is to build a library that ensures resource
safety 
- it has a mix of concerns: file IO, iteration, "actual" logic. This limits reuse/composibility

Other variations:
- Check whether the number of *non-empty* lines is > 40,000
- Find a line index before 40,000 where the first letters on consecutive lines spell out a particular word.

Compare this to how we'd solve this with a `Stream[String]`:

{%highlight scala%}
lines.zipWithIndex.exists(_.2 + 1 >= 40000)

lines.filter(!_.trim.isEmpty)
    .zipWithIndex
    .exists(_.2 + 1 >= 40000)

lines.filter(!_.trim.isEmpty)
    .take(40000)
    .map(_.head)
    .indexOfSlice("abracadabra".toList)
{%endhighlight%}

But, we don't have a `Stream[String]`, but a file...

A cheat using *lazy I/O*:
{%highlight scala%}
def lines(filename:String}:IO[Stream[String]] = IO {
    val src = io.Source.fromFile(filename)
    src.getLines.toStream append {src.close; Stream.empty;}
{%endhighlight%}

This is a cheat because the value inside (the `Stream`) isn't a pure value - it executes the side-effect of readng the
file. Also, the file resource is **only** closed when we read to the end of the stream - problematic for cases where we
wish to terminate early.

There's also nothing to prevent traversal of the stream more than once. Since we are executing some side-effecting IO,
we may run into problems when there are multiple threads processig the same stream.

In order to process the `Stream` safely, we'd need to have a good idea of where it comes from, where/how it's being
used, etc. 

> This is bad for composition, where we shouldn't have to know anything about the value other than its type

## Simple stream transducers

Stream transducers specify transformations from one stream to another. We're using the term "stream" loosely to talk
about *anything* that might emit a stream of data - could be from a file, http, etc.

Define a `Process` type that allows us to express these transformations:

{%highlight scala%}
sealed trait Process[I,O]

//We use a default value for tail of Halt() so that we can use Emit(x) 
case class Emit[I,O](head:O,
                    tail:Process[I,O] = Halt[I,O]()) extends Process[I,O]

case class Await[I,O](recv:Option[I] => Process[I,O]) extends Process[I,O]

case class Halt[I,O]() => extends Process[I,O]
{%endhighlight%}

What we have is a sort of state-machine which can be in one of three states:

* `Emit(head,tail`: emit the head value to the output stream and transition to the tail state
* `Await(recv)`: request a value from the input stream (which may or may not be present) and use the `recv` function to
determine the next state
* `Halt`: we're done. There are no more values to be read or emitted.

The state-machine is driven via a *driver* function that consumes our `Process` "program" and the input stream.

For a `Stream`, the driver function would look something like this (method on `Process`):

{%highlight scala linenos%}
def apply(s:Stream[I}):Stream[O] = this match {
    case Halt() => Stream() //we're done
    case Await(recv) => s match {
        case h #:: t => recv(Some(h))(t)
        case xs => recv(None)(xs)
    case Emit(h,t) => h #:: t(s)
{%endhighlight%}

Logic for lines 3-5 above. If `s` is a `Stream` with a head and more values (`t`), evaluate `recv` on `h`, and pass
remainder of the `Stream`, `t`, to the result (which will be another `Process`). If there are no more values in the
input stream (`xs`), then evaluate `recv` with `None` and pass the empty stream to the resulting `Process`.

## Creating Processes

We can take any function `I => O` and *lift* it into a 'Process[I,O]` by defining a function as follows:

{%highlight scala%}
def liftOne[I,O](f:I => O):Process[I,O] = 
    Await {
        case Some(i) => Emit(f(i))
        case None => Halt()
    }
}
{%endhighlight%}

This function just waits for a value to be emitted, applies the function `f` to it and emits the result. To transform an
entire stream, we just repleat this.

{%highlight scala%}
def repeat:Process[I,O] = {
    def go(p:Process[I,O]:Process[I,O] = p match {
        case Halt() => go(this) //if the process halts, restart it. i.e. liftOne completes
        case Await(recv) => Await {
            case None => recv(None) //if there are no more values, don't repeat, evaluate recv and return
            case i => go(recv(i))
            }
        case Emit(h,t) => Emit(h,go(t))
    }
    go(this)
}
{%endhighlight%}

...then to define a `lift` function that transforms the entire stream:

{%highlight scala%}
def lift[I,O](f:I => O):Process[I,O] = 
    liftOne(f).repeat
{%endhighlight%} 

We can define other useful combinators. For example, a *filter* `Process` that filters out elements that don't match a
predicate:

{%highlight scala%}
def filter[I](p: I => Boolean):Process[I,I] = 
    Await[I,I] {
        case Some(i) if p(i) => emit(i)
        case _ => Halt()
    }.repeat
    {%endhighlight%}

    ...or `sum`:

{%highlight scala%}
def sum:Process[Double,Double] = 
    def go(acc:Double):Process[Double,Double] = 
        Await {
            case Some(d) => Emit(d + acc, go(d +acc))
            case None => Halt()
        }
    go(0.0)
{%endhighlight%}

Other combinators:

- `take[I](n:Int):Process[I,I]`
- `drop[I](n:Int):Process[I,I]`
- `takeWhile[I](p:I => Boolean):Process[I,I]`
- `dropWhile[I](p:I => Boolean):Process[I,I]`
- `count[I]:Process[I,Int]`
- `mean:Process[Double,Double]`

The pattern from the implementaion of `sum` appears in the implementation of other combinators - we have some state (in
this case, the accumulator) that gets updated. We can pull out a `loop` function.

{%highlight scala%}
def loop[S,I,O](z:S)(f:(I,S) => (O,S)):Process[I,O] =
    Await((i:I) => f(i,z) match {
        case(O,s2) => emit(o,loop(s2)(f))
    })
{%endhighlight%} 


## Composing and Appending Processes

This is where we were trying to get to (getting back to a place where we can compose our functions). Given two
Processes, `f` and `g`, we want to pass the output of `f` to the input of `g`. 

{%highlight scala linenos%}
def |>[O2](g: Process[O,O2]): Process[I,O2] = g match {
  case Halt() => Halt() //if p2 is a halt, don't bother
  case Emit(h,t) => Emit(h, this |> t) //if p2 emits, emit the value and compose this with the tail
  case Await(recv) => this match { //recv is Option[I] => Process[I,O]
    case Halt() => Halt() |> recv(None) //if this is a halt, pass None to recv
    case Emit(h,t) => t |> recv(Some(h))
    case Await(r) => Await(i  => r(i) |> g)
  }
}
{%endhighlight%}

*[Note: this is defined on `Process` trait itself]*

- If `g` halts, then we just halt - we don't need to bother with `f` (`this`). 
- If `g` emits, we emit the value and compose `f` with the tail of `g`
- If `g` awaits, we examine `f`:
    - If `f` halts, we `Halt` and compose with the result of `recv(None)`
    - If `f` emits, we take the value emitted by `f` and pass it to the `recv` function (from `g`) and compose with the
    tail
    - If `f` awaits, when we get the value back, we evaluate `r` and compose the result with `g`

`f |> g` *fuses* transformations - as soon as `f` emits a value, it's tranformed by `g`. This means we can pipeline a
sequence of transformations.

For example, we can define `map` as follows:

{%highlight scala%}
def map[O2](f:O => O2):Process[I,O2] = this |> lift(f)
{%endhighlight%}

Many of the usual operations defined for sequences can also be defined for `Process`.

{%highlight scala linenos%}
def ++(p: => Process[I,O]):Process[I,O] = this match {
    case Halt() => p //the first Process has terminated, continue with the second
    case Emit(h,t) => Emit(h,t ++ p)
    case Await(recv) => Await(recv andThen (_ ++ p))
}
{%endhighlight%}

Line 4: if this is an `Await`, construct a new `Await` where we evaluate `recv` and append the result to `p`.

We can use `++` to implement `flatMap`.

{%highlight scala%}
def flatMap[O2](f: O => Process[I,O2]):Process[I,O2] = this match {
    case Halt() => Halt()
    case Emit(h,t) => f(h) ++ t.flatMap(f)
    case Await(recv) => Await(recv andThen (_ flatMap f))
}
{%endhighlight%}

We can form a `Process` monad by defining a monad instance as follows (note: we have to use the partially applied type
parameter trick):

{%highlight scala%}
def monad[I]:Monad[({type f[x] = Process[I,x]})#f] =
    new Monad[({type f[x] = Process[I,x]})#f] {
        def unit[O](o: => O):Process[I,O] = Emit(o)
        def flatMap[O,O2](p:Process[I,O])(f:O => Process[I,O2]):Process[I,O2] =
            p flatMap f
    }
{%endhighlight%}

## File processing

Now we can go back to our original example, file processing:

{%highlight scala%}
def processFile[A,B](f:java.io.File,
                     p:Process[String,A],
                     z:B)(g:(B,A) => B):IO[B] = IO {
    @annotation.tailrec
    def go(ss:Iterator[String],curr:Process[String,A],acc:B):B =
        curr match {
            case Halt() => acc
            case Await(recv) =>
                val next = if (ss.hasNext) recv(Some(ss.next))
                           else recv(None)
                go(ss,next,acc)
            case Emit(h,t) => go(ss,t,g(acc,h))
        }
    val s = io.Source.fromFile(f)
    try go(s.getLines, p, z)
    finally s.close
} 
{%endhighlight%}

## An Extensible Process type

As implemented, `Process` can only be one of three "instructions": `Halt`, `Emit`, or `Await`. To make `Process` more
extensible, we can parameterize the type. Note that this is very similar to what we did for `Async` with `Free`.

{%highlight scala linenos%}
trait Process[F[_],O]

object Process {
    case class Await[F[_],O](
        req:F[A],
        recv:Either[Throwable,A] => Process[F,O]) extends Process[F,O]
    case class Emit[F[_],O](
        head:O,
        tail:Process[F,O]) extends Process[F,O]
    case class Halt[F[_],O](err:Throwable) extends Process[F,O]

    case object End extends Exception
    case object Kill extends Exception
}
{%endhighlight%}

Notes:
    
- `recv` on line 6 takes an `Either` so that we can handle errors
- `Halt` takes a throwable now, we define `End` to indicate "normal" termination and `Kill` to indicate forceful
termination

There are some operations that we can define for this `Process` type that are independent of the choice of `F`.

- `map`
- `filter`
- `++` (append)

These operations can be implemented via some helper functions:

{%highlight scala%}
def Try[F[_],O](p: => Process[F,O]):Process[F,O] =
    try p
    catch { case e:Throwable => Halt(e) }

def onHalt(f:Throwable => Process[F,O]):Process[F,O] = this match {
    case Halt(e) => Try(f(e))
    case Emit(h,t) => Emit(h,t.onHalt f)
    case Await(req,recv) => Await(req, recv andThen (_.onHalt(f)))
{%endhighlight%}

So, `++` for example:

{%highlight scala%}
def ++(p: => Process[F,O]):Process[F,O] =
    this.onHalt {
        case End => p
        case err => Halt(err)
}
{%endhighlight%}

We can also define a helper function for `await`, which is curried for better type inference:

{%highlight scala%}
def await[F[_],A,O](req:F[A])(recv:Either[Throwable,A] => Process[F,O]):Process[F,O] =
    Await(req,recv)
{%endhighlight%}

FlatMap:

{%highlight scala %}
def flatMap[O2](f: O => Process[F,O2]):Process[F,O2] = 
    this match {
        case Halt(err) => Halt(err)
        case Emit(o,t) => Try(f(o)) ++ t.flatMap(f)
        case Await(req,recv) =>
            Await(req, recv andThen (_ flatMap f))
    }
{% endhighlight %}

## Sources

Now we can revisit the file processing example. In this case, our `F` will be some `IO`.

If we subsituted `F` for `IO`, this is what `Await` would look like:

{% highlight scala %}
case class Await[A,O](
    req:IO[A],
    recv:Either[Throwable,A] => Process[IO,O]
    ) extends Process[IO,O]
{% endhighlight %}

We can do the usual flatMap-y things in `req` (`IO[A]`) and handle the result (which will be either an error or a value)
in `recv`.

A simple(?) implementation of an interpreter:

{% highlight scala linenos %}
def runLog[O](src:Process[IO,O]):IO[IndexedSeq[O]] = IO {
    val E = java.util.concurrent.Executors.newFixedThreadPool(4)
    @annotation.tailrec
    def go(curr:Process[IO,O], acc:IndexedSeq[O]):IndexedSeq[O] =
        curr match {
            case Emit(h,t) => go(t, acc :+ h)
            case Halt(End) => acc //we're done
            case Halt(err) => throw err
            case Await(req,recv) => 
                val next = 
                    try recv(Right(unsafePerformIO(req)(E)))
                    catch { case err:Throwable => recv(Left(err)) }
                go(next,acc)
        }
    try go(src,IndexedSeq())
    finally E.shutdown
}
{% endhighlight %}

Mostly straightforward. In the `Await` case (line 9), we peform some IO operation based on the `req` (and using our
threadpool) which can potentially fail. In either case, we let `recv` decide what the next state should be and continue
by calling `go` with that next state.

We can define `runLog` more generally for any monad where catching and raising exceptions can occur.

{% highlight scala %}
trait MonadCatch[F[_]] extends Monad[F] {
  def attempt[A](a: F[A]): F[Either[Throwable,A]]
  def fail[A](t: Throwable): F[A]
}

def runLog(implicit F: MonadCatch[F]): F[IndexedSeq[O]] = {
  def go(curr:Process[F,O], acc:IndexedSeq[O]):F[IndexedSeq[O]] =
    curr match {
      case Emit(h,t) => go(t, acc :+ h) 
      case Halt(End) => F.unit(acc)
      case Halt(err) => F.fail(err) 
      case Await(req,recv) => F.flatMap(F.attempt(req)) {e => go(Try(recv(e)), acc) } 
    }
  go(this,IndexedSeq())
  }
{% endhighlight %}

## Ensuring Resource Safety

If we're doing IO, we need to find a way to ensure resource safety. I.e. if we're processing a file, we need to ensure
that we close the file after we're done, etc.

> A producer should free any underlying resources as soon as it knows it has no further values to produce, whether due
> to normal exhaustion or an exception.

But the *consumer* may decide to terminate early, therefore:

> Any process d that consumes values from another process p must ensure that cleanup actions of p are run before d halts

The cases we need to consider:

- producer exhaustion, signaled by `End` when the source has no more values to emit
- forcible termination, singaled by `Kill` when the consumer of the process is finished consuming (possibly before
producer exhaustion)
- abnormal termination due to some error in either the producer or the consumer

In each of these cases, we will want to close our underlying resource.

We need to ensure that the `recv` function in the `Await` constructor **always** runs the current set of cleanup actions
whenever it receives a `Left` (i.e. some sort of termination). To do this, we'll introduce a new combinator,
`onComplete` which will allow us to append some logic to a `Process` that will be invoked regardess of how the Process
terminates.

{% highlight scala %}
def onComplete(p: => Process[F,O]):Process[F,O] =
    this.onHalt {
        case End => p.asFinalizer
        case err => p.asFinalizer ++ Halt(err)
    }
   
def asFinalizer: Process[F,O] = this match {
  case Emit(h, t) => Emit(h, t.asFinalizer)
  case Halt(e) => Halt(e)
  case Await(req,recv) => await(req) {
    case Left(Kill) => this.asFinalizer
    case x => recv(x)
  }
}
{% endhighlight %}

## Single-input processes

What does `F` need to be in order to work with `Process[I,O]`? We need an `F` that only makes requests for values of
type `I`.

{% highlight scala linenos %}
case class Is[I] {
    sealed trait f[X]
    val Get = new f[I]{}
}

type Process1[I,O] = Process[Is[I]#f,O]
{% endhighlight %}

To see how this works, substitute `Is[I]#f` in for `F` in `Await`:

{% highlight scala %}
case class Await[A,O](
    req:Is[I]#f[A],
    recv:Either[Throwable,A] => Process[Is[I]#f,O]) extends Process[Is[I]#f,O]
{% endhighlight %}

`Is[I]#f` can *only* be `Get:f[I]` (sealed trait with only a single instance) - and `I` can only be `A`, which means
that `Await` can only be used to `req` and `recv` values of type `I`.

Some helper functions we can introduce for better type inference:

{% highlight scala %}
def await1[I,O](
    recv: I => Process1[I,O],
    fallback: Process1[I,O] = halt1[I,O]): Process1[I,O] =
        Await(Get[I], (e:Either[Throwable,I]) => e match {
            case Left(End) => fallback
            case Left(err) => Halt(err)
            case Right(i) => Try(recv(1))
        })

def emit1[I,O](h:O,t:Process1[I,O] = halt[I,O]): Process1[I,O] =
    emit(h,t)

def halt1[I,O]:Process1[I,O] = Halt[Is[I]#f, O](End)
{% endhighlight %}

Many of the combinators like `lift` and `filter` will look very similar to the previous implementations, except that
these will return `Process1` instead.

Process composition now looks like this:

{% highlight scala linenos %}
def |>[O2](p2: Process1[O,O2]): Process[F,O2] = {
  p2 match {
    case Halt(e) => this.kill onHalt { e2 => Halt(e) ++ Halt(e2) }
    case Emit(h, t) => Emit(h, this |> t)
    case Await(req,recv) => this match {
      case Halt(err) => Halt(err) |> recv(Left(err))
      case Emit(h,t) => t |> Try(recv(Right(h)))
      case Await(req0,recv0) => await(req0)(recv0 andThen (_ |> p2))
    }
  }
}

@annotation.tailrec
final def kill[O2]: Process[F,O2] = this match {
  case Await(req,recv) => recv(Left(Kill)).drain.onHalt {
    case Kill => Halt(End) // we convert the `Kill` exception back to normal termination
    case e => Halt(e)
  }
  case Halt(e) => Halt(e)
  case Emit(h, t) => t.kill
}

final def drain[O2]: Process[F,O2] = this match {
  case Halt(e) => Halt(e)
  case Emit(h, t) => t.drain
  case Await(req,recv) => Await(req, recv andThen (_.drain))
}
{% endhighlight %}

