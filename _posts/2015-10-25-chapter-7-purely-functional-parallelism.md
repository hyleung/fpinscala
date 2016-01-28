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
        Par.unit(ints.headOption getOrElse 0)
    else {
        val (l,r) = ints.splitAt(ints.length / 2)
        Par.map2(sum(l), sum(r))(_ + _)
    }
{% endhighlight %}

This gives us a `Par[Int]` that we can invoke `Par.get` on to evaluate.

Let's look at what the trace would be for `sum(IndexedSeq(1,2,3,4))`:

{% highlight console %}
sum(IndexedSeq(1,2,3,4))

map2(
    sum(IndexedSeq(1,2)),
    sum(IndexedSeq(3,4)))(_ + _)

map2(
    map2(
        sum(IndexedSeq(1)),
        sum(IndexedSeq(2)))(_ + _),
    sum(IndexedSeq(3,4)))(_ + _)

map2(
    map2(
        unit(1),
        unit(2))(_ + _),
    sum(IndexedSeq(3,4)))(_ + _)
 
map2(
    map2(
        unit(1),
        unit(2))(_ + _),
    map2(
        sum(IndexedSeq(3)),
        sum(IndexedSeq(4)))(_ + )

map2(
    map2(
        unit(1),
        unit(2))(_ + _),
    map2(
        unit(3),
        unit(4))(_ + _))(_ + _)
{% endhighlight %}

Because `map2` is evaluated stictly (left-to-right), we actually end up "expanding" the entire "left"
side/branch of the computation completely before moving onto the "right" side/branch. Even
if we evalutate things in parallel, we'd still end up beginning the left computation
before the right computation is even constructed.

We *could* keep `map2` strict but not actually execute the computation - basically
constructing a data structure that *describes* the computation. We would then evaluate
this "description". Problem is, this "description" will almost certainly require more
space than the original list.

Looks like we should make `map2` lazy and have it begin evaluating both sides in parallel.

### Explicit forking

It might not always be the case that we'll want to create a thread (for example) to
perform a computation. Something like `map2(unit(1), unit(1))(_ + _)`, for example - it's
probably not worth the trouble.

We can invent a function to allow the programmer to explicitly control when forking
is/isn't required.

{% highlight scala %}
def fork[A](a: => Par[A]):Par[A]
{% endhighlight %}

Then we can define `sum` as follows:
{% highlight scala %}
def sum(ints:Seq[Int]):Par[Int] =
    if (ints.size <= 1)
        Par.unit(ints.headOption getOrElse 0)
    else {
        val (l,r) = ints.splitAt(ints.length / 2)
        Par.map2(Par.fork(sum(l)), Par.fork(sum(r)))(_ + _)
    }
{% endhighlight %}

...so in the unit case, we don't bother with forking, but we do in the case where there
are more than (in this case) one Pars to compute.

By introducing `fork`, we have a separation of concerns - combining the results of two
parallel tasks (`map2`) and performing a computation asynchronously (that's `fork`).
`map2` can be strict, and if the programmer wishes to perform some work asynchronously,
they can do so by wrapping the work using `fork`.

Introducing a `fork` function also means that the programmer can opt to make `unit` lazy
by wrapping it in `fork`:

{% highlight scala %}
def unit[A](a:A):Par[A]
def lazyUnit[A](a: => A):Par[A] = fork(unit(a))
{% endhighlight %}

### What should `fork` do?

We've introduced `fork` to indicate that a computation *should* take place in parallel - but should it be the
responsibility of `fork` itself to make this happen? Or should it be the responsiblity of some other method that
evaluates the program (like a `get`)? 

Could go either way, but one thing to consider is that if `fork` we responsible for this, it would need to "know" how to
create threads, etc. This is not necessarily bad, but it is a bit more restricting. It's possilble that there isn't a
globally appropriate way to do this. So we can go with the alternative, which is to make it the responsibility of some
`get` function - which we'll call `run'
 
{% highlight scala %}
def run[A]:(a:Par[A]):A
{% endhighlight %}

### The API so far...

Up to this point (**note: this was *without* any implementation, only by playing with functions**), we've arrived at the
following API for `Par`:

{% highlight scala %}
def unit[A](a:A):Par[A]
def map2[A,B,C](pa:Par[A], pb:Par[B])(f:(A,B) => C):Par[C]
def fork[A](a: => Par[A]):Par[A]
def lazyUnit[A](a: => A):Par[A] = fork(unit(a))
def run[A](a:Par[A]):A
{% endhighlight %}

### Concrete Implementaton of our API
Alright, so let's start looking at a concrete implementation of our API. Our `run` function needs to know how to perform
some work asynchronously. How about using `java.util.concurrent.ExecutorService`?

In Scala, the API for `ExecutorService` looks something like this:

{% highlight scala %}
class ExecutorService {
    def submit[A](a:Callable[A]):Future[A]
}

trait Callable[A] {
    def call:A
}

trait Future[A] P
    def get:A
    def get(timeout:Long, unit:TimeUnit):A
    def cancel(evenIfRunning:Boolean):Boolean
    def isDone:Boolean
    def isCancelled:Boolean
}
{% endhighlight %}

So, our `run` would need access to an `ExecutorService`:

{% highlight scala %}
def run[A](executor:ExecutorService)(p:Par[A]):A
{% endhighlight %}

What does that make `Par`? We could go with `ExecutorService => A`. But we might want to have control over whether we
want to cancel the computation, or set a timeout - i.e. we probably want:

{% highlight scala %}
type Par[A] = ExecutorService => Future[A]

def run[A](executor:ExecutorService)(p:Par[A]): Future[A] = p(executor)
{% endhighlight %}

...we return a `Future` that the caller can call `get` on, with a timeout if necessary, cancel, etc.

