---
layout: post
title:  "Chapter 13: External Effects and IO"
date:   2015-11-01 20:32:40 -0700
categories: fpinscala chapter_notes
---

# External Effects and IO

The goal here is to provide a…*way of embedding imperative programming with I/O effects in a pure program while **preserving referential transparency**.*

We separate the code that has *external effects*, from code that does not.

## Factoring effects

{% highlight scala %}
case class Player(name:String, score:Int)

def contest(p1: Player, p2: Player):Unit =
  if (p1.score > p2.score)
    println(s"${p1.name} is the winner!")
  else if (p2.score > p1.score)
    println(s"${p2.name} is the winner!")
  else
    println("It's a draw!")    
{% endhighlight %}

In the preceding example, we have side effects - that is `println`, which is I/O - mixed in with the pure logic.

We can move things in the right direction by factoring out the logic (i.e. who wins) into a separate function:

{% highlight scala %}
def winner(p1: Player, p2: Player):Option[Player] {
  if (p1.score > p2.score) Some(p1)
  else if (p2.score > p1.score) Some(p2)
  else None
}

def contest(p1: Player, p2: Player):Unit = winner(p1, p2) match {
  case Some(p) => println(s"${p.name} is the winner!")
  case None => println("It's a draw!")
}
{% endhighlight %}

This is *a bit* better. But `contest` still has two responsibilities here: computing the message to be displayed and *actually* displaying the message (printing it to console). We can refactor this further:

{% highlight scala %}
def computeMessage(p:Option[Player]):String = p map {
  case Player(name,_) => s"$name is the winner!"
} getOrElse "It's a draw!"

def contest(p1: Player, p2: Player):Unit =
  println(computeMessage(winner(p1, p2)))
{% endhighlight %}

We can take this a step further and create a type to represent the IO:

{% highlight scala %}
trait IO { def run:Unit }

def printLine(msg:String):IO = new IO{ def run = println(msg) }

def contest(p1: Player, p2: Player):IO =
  printLine(computeMessage(winner(p1, p2)))
{% endhighlight %}

Given an *impure* function, `f: A => B`, we can factor this as function, `A => D`, where `D` is some *description* of the result of `f` (`computeMessage`) and `D => B`, an *interpreter* of this result.

We can flesh out this `IO` trait by adding some operations:

{% highlight scala %}
trait IO { self =>
  def run:Unit
  def ++(io:IO):IO = new IO {
    def run = {
      self.run
      io.run
    }
  }
}

object IO {
  def empty:IO = new IO { def run = () }
}
{%endhighlight%}

i.e. we have formed a `Monoid` for our `IO` type.

## Input effects

The preceding IO trait, doesn't allow our effects to return any value. Let's fix that:

{% highlight scala %}
trait IO[A] { self =>
  def run:A
  def map[B](f:A => B):IO[B] =
    new IO[B] { def run = f(self.run)}
  def flatMap[B](f:A => IO[B]):IO[B] =
    new IO[B] { def run = f(self.run).run }
}
{% endhighlight %}

What do we get by introducing this IO type?

- IO computations are just *values*. We can keep lists of them, create new ones, re-use them, etc.
- we can come up with more interesting interpreters for IO computations

But...(with the simple implementation above):

- both the `map` and `flatMap` implementations aren't tail recursive, so it isn't stack safe
- the `IO[A]` type is opaque. We have no way of knowing what the program will do. We can `map` and `flatMap`, etc. and `run`.

### Addressing the tail recursion issue

We can address the tail recursion issue by *reifying* the control flow into our type.

{% highlight scala %}
sealed trait IO[A] {
  def flatMap[B](f:A => IO[B]):IO[B] =
    FlatMap(this, f)
  def map[B](f:A => B):IO[B] =
    flatMap(f andThen (Return(_)))
}
//Just return a value
case class Return[A](a:A) extends IO[A]
//Suspend a computation, return a result when resumed
case class Suspend[A](resume:() => A) extends IO[A]
//Process the sub-computation, then continue with k once "sub" produces a result
case class FlatMap[A,B](sub:IO[A], k: A => IO[B]) extends IO[B]

{% endhighlight %}

The `FlatMap` data constructor lets us extend or continue an existing computation.

The interpreter (runner) implementation for this type:

{%highlight scala%}

@annotation.tailrec def run[A](io: IO[A]): A = io match {
  case Return(a) => a
  case Suspend(r) => r()
  case FlatMap(x, f) => x match {
    case Return(a) => run(f(a))
    case Suspend(r) => run(f(r()))
    case FlatMap(y, g) => run(y flatMap (a => g(a) flatMap f))
  }
}
{% endhighlight %}
...which *is* tail recursive.

What's going on in the FlatMap case?

We actually have:

`FlatMap(FlatMap(y,g),f)`

The next time we hit `run`, we actually want to check if `y` is another `FlatMap` - so we use the associativity law for monads to "swap" things around:

`FlatMap(FlatMap(y,g),f)` is actually equivalent to `(y flatMap g) flatMap f`. Using associativity, we know that this is equivalent to: `y flatMap (a => g(a) flatMap f)`.

The `run` function is called a *trampoline*

## Trampolining

In the `IO` trait above, there wasn't really any IO going on - it was just a monad that we used for tail-call elimination. So we can rename it:

{% highlight scala %}
sealed trait TailRec[A] {
  def flatMap[B](f:A => TailRec[B]):TailRec[B] =
    FlatMap(this, f)
  def map[B](f:A => B):TailRec[B] =
    flatMap(f andThen (Return(_)))
}

//Just return a value
case class Return[A](a:A) extends TailRec[A]
//Suspend a computation, return a result when resumed
case class Suspend[A](resume:() => A) extends TailRec[A]
//Process the sub-computation, then continue with k once "sub" produces a result
case class FlatMap[A,B](sub:TailRec[A], k: A => TailRec[B]) extends TailRec[B]
{% endhighlight %}

Here's an example where we'd get a `StackOverflowError`:

{%highlight console %}
val f = (x:Int) => x
f: Int => Int = <function1>

val g = List.fill(100000)(f).foldLeft(f)(_ compose _)
g: Int => Int = <function1>

g(42)
java.lang.StackOverflowError
{%endhighlight%}

`g` is taking 100000 `f` and composing them together, but when we try to execute `g` we get a `StackOverflowError`.

We can fix this by using `TailRec` to compose the functions:

{%highlight console %}
val f:Int => TailRec[Int] = (x:Int) => Return(x)
f: Int => TailRec[Int] = <function1>

val g = List.fill(100000)(f).foldLeft(f){
  | (a,b) => x => Suspend(() => a(x).flatMap(b))
  | }
g: Int => TailRec[Int] = <function1>

val x = run(g(42))
x: Int = 42
{%endhighlight%}

Note how we started off with trying to compose `A => B`, but ended up using `A => M[B]`. That's a *Kleisli function* - we're using *Kleisli composition* instead of regular function composition.

## Defining an Async type using Par

We can use the `Par` type that was developed in Chapter 7 to implement something that can support asynchronous execution:

{% highlight scala %}
sealed trait Async[A] {
  def flatMap[B](f: A => Async[B]):Async[B] =
    FlatMap(this,f)
  def map[B](f: A => B):Async[B] =
    flatMap(f andThen (Return(_))
}

case class Return[A](a:A) extends Async[A]
case class Suspend[A](resume:Par[A]) extends Async[A]
case class FlatMap[A,B](sub:Async[A], k:A => Async[B]):Async[B]
{% endhighlight %}

We use a tail-recursive `step` function to help us implement the `run`:

{% highlight scala %}
@annotation.tailrec
def step[A](async:Async[A]):Async[A] = async match {
  //this is the same right associativity trick from above
  case FlatMap(FlatMap(x,f),g) => step(x flatMap (a => f(a) flatMap g))  
  case FlatMap(Return(x),f) => step(f(x))
  case _ => async
}

def run[A](async:Async[A]):Par[A] = step(async) match {
  case Return(a) => Par.unit(a)
  case Suspend(s) => Par.flatMap(s)(a => run(a))
  case FlatMap(x,f) => x match {
    case Suspend(r) => Par.flatMap(r)(a => run(f(a))
    case _ => sys.error("Ruh-roh!") //shouldn't happen
  }
}
{% endhighlight %}

We can take this a step further by abstracting out the `Async` or `TailRec` part and parameterizing on some type constructor `F`, like we did in the preceding chapters:

{% highlight scala %}
sealed trait Free[F[_],A]
case class Return[F[_],A](a:A) extends Free[F,A]
case class Suspend[F[_],A](s:F[A]) extends Free[F,A]
case class FlatMap[F[_],A,B](s:Free[F,A],f: A => Free[F,B]) extends Free[F,B]
{% endhighlight %}

`TailRec` and `Async` are then type aliases:

{% highlight scala %}
type TailRec[A] = Free[Function0,A]
type Async[A] = Free[Par,A]
{% endhighlight %}

`Free[F,A]` is a *recursive* structure containing some `A` wrapped in layers of `F`. Can be thought of as a way to construct an *abstract syntax tree*, describing a program and how it branches, etc.

**More on free monads**

- [Many Roads to Free Monads](https://www.fpcomplete.com/user/dolio/many-roads-to-free-monads)
- [Why Free Monads Matter](http://www.haskellforall.com/2012/06/you-could-have-invented-free-monads.html)
- [Learning Scalaz - Free Monad](http://eed3si9n.com/learning-scalaz/Free+Monad.html)

## Console I/O

{% highlight scala %}
sealed trait Console[A] {
  def toPar:Par[A]
  def toThunk:() => A
}

case object Readline extends Console[Option[String]] {
  def toPar = Par.lazyUnit(run)
  def toThunk = () => run

  def run:Option[String] =
    try Some(readLine())
    catch { case e:Exception => None}
}

case class PrintLine(line:String) extends Console[Unit] {
  def toPar = Par.lazyUnit(println(line))
  def toThunk = () => println(line)
}
{% endhighlight %}

The signature for `run`:

{% highlight scala%}
def run[F[_],A](a:Free[F,A])(implicit F:Monad[F]):F[A]
{%endhighlight%}

But we can't implement `flatMap` for `Console`:

{%highlight scala%}
sealed trait Console[A] {
  def flatMap[B](f:A => Console[B]):Console[B] = this match {
    case ReadLine => ???
    case PrintLine(s) => ???
  }
}
{%endhighlight%}

So we need to *translate* `Console` into some other type that can form a Monad.

{%highlight scala%}
trait Translate[F[_],G[_]] {
  def apply(f:F[A]):G[A]
}
//infix operator?
type ~>[F[_],G[_]] = Translate[F,G]
{%endhighlight%}

Both `Function0` and `Par` can form Monads so we'll implement translations from `Console` to these:

{%highlight scala%}
val consoleToFunction0 =
  new (Console ~> Function0) { def apply[A](a:Console[A]) = a.toThunk }

val consoleToPar =
  new (Console ~> Par) { def apply[A](a:Console[A]) = a.toPar }
{%endhighlight%}

We can generalize `runFree` so that it does the translation as we interpret the program:

{%highlight scala%}
def runFree[F[_],G[_]](free:Free[F,A])(translation:F ~> G)(implicit G:Monad[G]):G[A] =
  step(free) match {
    case Return(a) => G.unit(a)
    case Suspend(r) => t(r)
    case FlatMap(Suspend(r),f) => G.flatMap(t(r))(a => runFree(f(a))(t))
    case _ => sys.error("Ruh-roh, step should eliminate this case")
  }
{%endhighlight%}

We can run the `Console` program as either `Function0` or `Par`:

{%highlight scala%}
def runFreeConsoleFunction0[A](a:Free[Console,A]): () => A =
    runFree[Console,Function0,A](a)(consoleToFunction0)

def runFreeConsolePar[A](a:Free[Console,A]):Par[A] =
    runFree[Console,Par,A](a)(consoleToPar)
{%endhighlight%}

...given that we have the necessary bits to form the `Function0` and `Par` monads:

{%highlight scala%}
implicit val function0Monad = new Monad[Function0] {
  def unit[A](a: => A) = () => A
  def flatMap[A,B](a:Function0[A])(f: A => Function0[B]) =
    () => f(a())()
}

implicit val parMonad = new Monad[Par] {
  def unit[A](a: => A) = Par.unit(a)
  def flatMap[A,B](a:Par[A])(f: a => Par[B]) =
    Par.fork { Par.flatMap(a)(f) }
}    
{%endhighlight%}
