---
layout: post
title:  "Chapter 14: Local Effects and Mutable State"
date:   2015-11-02  20:32:40 -0700
categories: fpinscala chapter_notes
---

# Local Effects and Mutable State

Some definitions:
    
**Referential Transparency**:

*"An expression `e` is referentially transparent if for all programs `p` all occurences of
`e` can be replaced by the result of evaluating `e` without affecting the meaning of `p`"*

**Pure functions**: 

*"A function `f` is pure if the expression `f(x)` is referentially transparent for all referentially
transparent `x`*

...but notice neither definition says anything about mutable state.

*Locally scoped* mutable state is ok as long as the above definition(s) hold.

As long as that mutable state doesn't *"escape"* such that it is observable to outside code, we're ok.

We can use Scala's type system to enforce scoping.

## ST Monad and scoped mutability

The goal - our code should not compile if either of these invariants don't hold:

- if a reference is held to the mutable object, nothing can observe mutations of state
- the mutable object can never be observed outside of the scope in which it was created

Call this "mutable state" monad, `ST` (for *state thread*, or *state transition*, *state token* or *state tag*)

Our `ST` Monad:

{% highlight scala %}
sealed trait ST[S,A] { self =>
    protected def run(s:S):(A,S)

    def map[B](f: A => B): ST[S,B] = new ST[S,B] {
        def run(s:S) = {
            val (a, s1) = self.run(s)
            (f(a), s1)
        }
    }
    
    def flatMap[B](f: A => ST[S,B]): ST[S,B] = new ST[S,B] {
        def run(s:S) = {
            val (a, s1) = self.run(s)
            f(a).run(s1)
        }
    }
}

object ST {
    def apply[S,A](a: => A) = {
        lazy val memo = a
        new ST[S,A] {
            def run(s:S) = (memo, s)
        }
    }
}
{% endhighlight %}

This is almost the same as the regular `State` monad, execept the `run` method is `proected` here.

We can use thie `ST` type to implement a type for mutable references:

{% highlight scala %}
sealed trait STRef[S,A] {
    protected var cell:A
    def read:ST[S,A] = ST(cell)

    def write(a:A):ST[S,Unit] = new ST[S,Unit] {
        def run(s:S) = {
            cell = a
            ((),s)
        }
    }
}

object STRef {
    def apply[S,A](a:A): ST[S, STRef[S,A]] = ST(new STRef[S,A] {
        var cell = a
    })
}
{% endhighlight %}

Since `STRef` is sealed, the `apply` function on the companion object is the only way to create an `STRef` instance. And
the type that is actually returned is not a "naked" `STRef`, but an `ST[S, STRef[S,A]]`. The `STRef` is only produced
when `ST` is run. `S` is a value that serves as a sort of token.

So we can write a program to perform a bunch of `ST` operations:

{% highlight scala %}
for {
    r1 <- STRef[Nothing,Int](1)
    r2 <- STRef[Nothing,Int](1)
    x  <- r1.read
    y  <- r2.read
    _  <- r1.write(y+1)
    _  <- r2.write(x+1)
    a  <- r1.read
    b  <- r2.read
} yield (a,b)
{% endhighlight %}

So how do we run this "program"?




