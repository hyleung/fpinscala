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



