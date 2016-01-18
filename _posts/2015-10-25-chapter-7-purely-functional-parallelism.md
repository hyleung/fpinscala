---
layout: post
title:  "Chapter 7: Purely Functional Parallelism"
date:   2015-10-25 20:32:40 -0700
categories: fpinscala chapter_notes
---
# Purely Functional Parallelism

Here we'll build a library for parallel computing using only pure functions.

## Motivating Example: Summing a sequence of Ints

A simple implementation might look something like this:

{% highlight scala %}
def sum(ints:Seq[Int]):Int =
    ints.foldLeft(0)((a,b) => a + b)
{% endhighlight %}

What we'd *like* to do, is use a *divide-and-conquer* approach to this:

{% highlight scala %}
def sum(ints:Seq[Int]):Int =
    if (ints.size <= 1)
        ints.headOption getOrElse 0
    else {
        val (l,r) = ints.splitAt(ints.length/2)
        sum(l) + sum(r)
    }
{% endhighlight %}

## Data type

Without getting into an implementation, we can start with defining a type and some
operations on the type and get a sense of what we can/cannot express using the type.

Suppose we defined a `Par[A]` type, with the following:

{% highlight scala %}
def unit[A](a: => A):Par[A]

def get[A](a:Par[A]):A
{% endhighlight %}

`unit` is used to create a computation that returns some `A` when evaluated. `get` is used
to get the result from some parallel computation.

So, what does the "divide-an-conquer" version of `sum` look like now?

{% highlight scala %}
def sum(ints:Seq[Int]):Int =
    if (ints.size <= 1)
        ints.headOption getOrElse 0
    else {
        val (l,r) = ints.splitAt(ints.length / 2)
        val sumL:Par[Int] = Par.unit(sum(l))
        val sumR:Par[Int] = Par.unit(sum(r))
        Par.get(sumL) + Par.get(sumR)
    }
{% endhighlight %}

The problem here is that it's not *really* going to be evaluated in parallel. What will
happen is that we'll evaluate `Par.get(sumL)` *and then* `Par.get(sumR)`. 

So calling `Par.get` isn't an option. 

Suppose we had something like this:

{% highlight scala %}
def map2[A,B,C](pa:Par[A], pb:Par[B])(f:(A,B) => C):Par[C]
{% endhighlight %}

Our "divide-and-conquer" implementation becomes:

{% highlight scala %}
def sum(ints:Seq[Int]):Par[Int] =
    if (ints.size <= 1)
        ints.headOption getOrElse 0
    else {
        val (l,r) = ints.splitAt(ints.length / 2)
        Par.map2(sum(l), sum(r))(_ + _)
    }
{% endhighlight %}

This gives us a `Par[Int]` that we can invoke `Par.get` on to evaluate.
