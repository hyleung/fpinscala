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


