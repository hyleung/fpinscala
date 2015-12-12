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

`ST[S, STRef[S,Int]]` is **not** safe to run, whereas `ST[S,Int]` **is** safe to run. The former returns a mutable
reference, whereas the latter returns just a value (which may have been computed using mutable references along the
way). 

In the example above, we yield an `ST[S,(Int,Int)]`.

We want to disallow running any `ST[S,T]` where `T` has anything to do with `S`.

Define a trait to represent `ST` actions that are safe to run:

{%highlight scala%}
trait RunnableST[A] {
    def apply[S]:ST[S,A]
}
{%endhighlight%} 

In the previous example, we'd wrap the expression in a `RunnableST[A]`:

{% highlight scala %}
val p = new RunnableST[(Int,Int)] { 
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
}
{% endhighlight %}

We add a `runST` function to the companion object for `ST` - since `ST` is a sealed trait, `runST` will have access to
the protected `run` function:

{%highlight scala %}
object STRef {
    def apply[S,A](a:A): ST[S, STRef[S,A]] = ST(new STRef[S,A] {
        var cell = a
        })
    def runST[A](st: RunnableST[A]):A = 
        st.apply[Unit].run(())._1
}
{%endhighlight%}

We can use a similar approach to implement mutable arrays, maps, etc.

## Key Points

- Referential transparency is *with regard* to some context
- It's ok to have mutable state, provided that any (side) effects are non-observable
- An effect is *non-observable* if it doesn't affect referential transparency of an expression `e` with regard to some
program `p`

> An expression `e` is referentially transparent with regard to a program `p` if every occurence of `e` in `p` can be
> replaced by the result of evaluating `e` without affecting the meaning of `p`

*Some* effects aren't going to affect the meaning of `p`. We should track effects that have an impact on the correctness
of the program. For example, `println` probably isn't worth tracking in most cases.

> tracking effects is a *choice* we make as programmers. It's a value judgement, and there are trade-offs associated
> with how we choose. 


