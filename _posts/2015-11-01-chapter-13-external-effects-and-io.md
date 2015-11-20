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
