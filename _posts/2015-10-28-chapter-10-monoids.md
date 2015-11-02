---
layout: post
title:  "Chapter 10: Monoids"
date:   2015-10-28 20:32:40 -0700
categories: fpinscala chapter_notes 
---

A `Monoid` is an [algebraic structure](https://en.wikipedia.org/wiki/Algebraic_structure).

"Algebraic structure" => "a set with one or more finitary operations defined on it that satisfies a list of axioms."

"Finitary operation" => an operation that takes a finite number of input values and produces an output

A Monoid consists of:

- some type, `A`
- an binary operation that takes two values of `A` and combines them into one:
       ` op(a1:A,a2:A):A`
- a value, `zero` that returns `A`

Such that:

- `op` is *associative* => `op(a,op(b,c)) == op(op(a,b),c)`
- `zero` is an *identity* => `op(x,zero) == op(zero,x) == x` 

"The type `A` _forms_ a _monoid_ under the operations defined by the `Monoid[A]` instance"

Expressed as a Scala trait:

    trait Monoid[A] {
        def op(a1:A, a2:A):A
        def zero:A
    }

Some examples...

    val stringMonoid = new Monoid[String] {
        def op(s1:String, s2:String):String = s1 + s2
        def zero = ""
    }

    val listMonoid[A] = new Monoid[List[A]] {
        def op(l1:List[A], l2:List[A]):List[A] = l1 ++ l2
        def zero = Nil
    }

Other Monoid instances

- `intAddition:Monoid[Int]`
- `intMultiplication:Monoid[Int]`
- `booleanOr:Monoid[Boolean]`
- `booleanAnd:Monoid[Boolean]`
- `optionMonoid:Monoid[Option[A]]`
- `endoMonoid:Monoid[A=>A]` (an _endofunction_ is a function that has the same argument and return type)

Folding lists with monoids

Look at the signatures of `foldLeft` and `foldRight`.

    def foldLeft[B](z:B)(f:(B,A) => B):B

    def foldRight[B](z:B)(f:(A,B) => B):B

…if `A` and `B` are the same…

    def foldLeft[A](z:A)(f:(A,A) => A):A

    def foldRight[A](z:A)(f:(A,A) => A):A

Notice that `(z:A)` is `zero` and `f:(A,A) => A` is `op`.

…so we can reduce `foldLeft` and `foldRight` for Monoids to:

    def foldLeft[A](monoid.zero)(monoid.op):A

    def foldRight[A](monoid.zero)(monoid.op):A

Since _associativity_ and _identity_ hold for monoids, it doesn't matter which of the fold operations we use.

Assciativity and parallelism

Associativity allows us to chose how we fold over a datastructure (like a list).

`foldLeft` over `a,b,c,d`  would look like this:

    op(op(op(a,b),c),d)

Similarly, `foldRight` over `a,b,c,d` would look like this:

    op(a,op(b,op(c,d)))

When _associativity_ holds we can use a _**balanced**_ fold:

    op(op(a,b),op(c,d))

Using a balanced fold allows us to introduce parallelism, since the inner `op` functions can be evaluated independently of each other.

Signature of a balanced foldMap over an `IndexedSeq`:

    def foldMapV[A,B](v:IndexedSeq[A],m:Monoid[B])(f:A => B):B


