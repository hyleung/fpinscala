---
layout: post
title:  "Chapter 6: Purely Functional State"
date:   2015-10-24 20:32:40 -0700
categories: fpinscala chapter_notes
---
# Purely Functional State

## Example: Random number generation

Consider random number generation using `scala.util.Random`:

{% highlight console %}
scala> val rng = new scala.util.Random

scala> rng.nextDouble
res1:Double = ...

scala> rng:nextDouble
res2:Double = ...
{% endhighlight %}

There's obviously some internal state in `scala.util.Random` since we get a different
value every time. The state update is performed as a side-effect of invoking (in this
case) `nextDouble`, so these functions aren't referentially transparent. This means that,
consequently, we lose testability, modularity composability, etc.

## Purely functional approach

We'll define a trait for our random number generator:

{% highlight scala %}
trait RNG {
    def nextInt:(Int, RNG)
}
{% endhighlight %}

Instead of mutating the internal state in-place, we return the value (the random `Int` in
this case) along with the new state. Note that we're not exposing the internals of `RNG`,
that internal state and its representation is still encapsulated.

### Example implementation of RNG

{% highlight scala %}
case class Simple(seed: Long) extends RNG {
    def nextInt: (Int, RNG) = {
      val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL // `&` is bitwise AND. We use the current seed to generate a new seed.
      val nextRNG = Simple(newSeed) // The next state, which is an `RNG` instance created from the new seed.
      val n = (newSeed >>> 16).toInt // `>>>` is right binary shift with zero fill. The value `n` is our new pseudo-random integer.
      (n, nextRNG) // The return value is a tuple containing both a pseudo-random integer and the next `RNG` state.
    }
}
{% endhighlight %}

For a given `seed`, we'll always return the same `(n, nextRNG)`. And if we invoked `nextInt`
on the new state, we'd get the same `(n, nextRNG)` from **that** result. We have gotten back
to referential transparency.

## More general stateful APIs

Suppose you have a class:

{% highlight scala %}
class Foo {
    private var s:FooState = ...
    def bar:Bar
    def baz:Int
}
{% endhighlight %}

Maybe we try to apply the same pattern by using the same approach of passing along the
encapsulated state:

{% highlight scala %}
trait Foo {
    def bar:(Bar, Foo)
    def baz:(Int, Foo)
}
{% endhighlight %}

This works, but we do have to explicitly pass along the new state, which is a pain in the
ass. And often the caller will be repsonsible for doing that, and doing it properly.

## A better approach

A commonly recurring pattern in this code is `RNG => (A,RNG)` for some `A`. Functions liek
this are called *state actions* or *state transitions*.

Let's make a type alias for this:

{% highlight scala %}
type Rand[+A] = RNG => (A,RNG)
{% endhighlight %}

So a function that would have been defined as:

{% highlight scala %}
def randomInt(rnd:RND):(Int,RND)
{% endhighlight %}

...would turn into this:

{% highlight scala %}
val int:Rand[Int] = _.nextInt
{% endhighlight %}

We want to write combinators that let us compose different `Rand` actions *without* having
to pass around state.

Let's start with a combinator that just returns a constant value and passes through the
state:

{% highlight scala %}
def unit[A](a:A):Rand[A] =
    rng => (a,rng)
{% endhighlight %}

...then `map`, which transforms the value using some function `A => B`:

{% highlight scala %}
def map[A,B](s:Rand[A])(f:A => B):Rand[B] =
    rng => {
        val (a, nextRng) = s(rng)
        (f(a), nextRng) 
    }
{% endhighlight %}

...also `map2`, which takes two `Rand` and a function that operates on their values:

{% highlight scala %}
def map2[A,B,C](ra:Rand[A], rb:Rand[B])(f:(A,B) => C):Rand[C] =
    rng => {
        val (a,r1) = ra(rng)
        val (b,r2) = rb(r1)
        (f(a,b),r2)
    }
{% endhighlight %}

`map2` turns out to be a prety useful combnator. For example, here's a `both` combinator
that returns the pair of values:

{% highlight scala %}
def both[A,B](ra:Rand[A], rb:Rand[B]):Rand[(A,B)] =
    map2(ra,rb)((_,_))
{% endhighlight %}

Here's `sequence`, which takes a list of `Rand` and returns a `Rand[List]`:

{% highlight scala %}
def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs.foldRight(unit(List.empty[A])){ (f,acc) =>
      map2(f,acc)( (x,xs) => x :: xs)
    }
{% endhighlight %}

## Nesting State Actions

Example:

{% highlight scala %}
def nonNegativeLessThan(n:Int):Rand[Int] = { rng =>
    val (i,rng2) = nonNegative(rng)
    val mod = i % n
    if (i + (n - 1) - mod >= 0)
        (mod, rng2)
    else nonNegativeLessthan(n)(rng)
    }
{% endhighlight %}

This works but kind of sucks because we have to pass the state around.

We'd prefer to *not* have to do this...introducing `flatMap`.

{% highlight scala %}
def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] = rng => {
    val (x, next) = f(rng)
    g(x)(next)
}
{% endhighlight %}

Using `flatMap`, our `nonNegativeLessThan` becomes:

{% highlight scala %}
def nonNegativeLessThan(n:Int):Rand[Int] =
    flatMap(nonNegative){ i =>
        val mod = i % n
        if (i + (n - 1) - mod >= 0) unit(mod)
        else nonNegativeLessThan(n)
    }
{% endhighlight %}

We can also use `flatMap` to implement `map` and `map2`:

{% highlight scala %}
def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s){a => unit(f(a))}

def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => flatMap(rb)(b => unit(f(a,b))))
{% endhighlight %}

## A more general state action

The functions we've looked at so far actually don't necessarily have anything to do
*specifically* with random number generation. We can generalize them to allow us to use
them for *any* state action.

For example, look at the signature of `map` if we replace `Rand` with some generic `S`:

{% highlight scala %}
def map[S,A,B](a: S => (A,S))(f: A => B):S => (B,S)
{% endhighlight %}

We can define a type alias to make things a bit more readable:

{% highlight scala %}
type State[S,+A] = S => (A,S)
{% endhighlight %}

So our definition of `Rand` becomes:

{% highlight scala %}
type Rand[A] = State[[RNG,A]
{% endhighlight %}

Here's an implementation:

{% highlight scala %}
case class State[S,+A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] = flatMap(a => State.unit(f(a)))

  def map2[B,C](sb: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap{a => sb.map { b => f(a, b) }}

  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State{ (s:S) =>
      //get the value of type A and next state B
      val (a, s1) = run(s)
      /* f(a) returns a function A => State[S,B]
         so we need to evaluate the function to
         get an actual State[S,B]
       */
      f(a).run(s1)
    }
}
{% endhighlight %}

## Purely functional imperative programming

Using state action, we can achieve imperative-style programming, while retaining
referential transparency.

We can define two state actions:

{% highlight scala %}
def get[S]:State[S,S] = State(s => (s,s))

def set[S](s:S):State[S,Unit] = State(_ => ((),s))
{% endhighlight %}

This allows us to write programs like this:

{% highlight scala %}
def modify[S](f: S => S):State[S,Unit] = for {
    s <- get
    _ <- set(f(s))
    } yield()
{% endhighlight %}

This will get the current state and assign it to `s` and set the new state to be `f`
applied to `s`.


