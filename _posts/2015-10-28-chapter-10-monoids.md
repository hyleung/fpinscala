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

