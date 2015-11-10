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

Monoid Homomorphisms

"Homomorphism" - "same shape" (i.e. preserves the monoid structure)

Given monoids `M` and `N`, a monoid homomorphism is a function `f: M -> N`, where the monoid laws of associtivity and identity hold.

    M.op(f(x),f(y)) = f(N.op(x,y))

E.g. the function, `length` is a function from `String -> Int`. Taking the length of two strings and adding them together is the same as concatenating two strings and taking the length of the concatenated string. In the definition above, `M` would be `Int`, `N` would be `String` and `f` is `length`.

Two monoids are *isomorphic* if there exists:

    f: M -> N
    g: N -> M

such that:

    f andThen g

and

    g andThen f

...are identity functions

Foldable Data structures

Sometimes we want to process the data contained in a data structure (e.g. a tree, list, etc.) and we don't particularly care about the shape or characteristics of the data structure.

E.g. we have a structure full of integers, and we want to compute the sum.

    ints.foldRight(0)(_ + _)

We don't really care whether the data structure is a `List` or `Stream` or whatever.

We can capture this (and other useful functions) by defining a `Foldable` trait in Scala.

```
trait Foldable[F[_]] {
  def foldRight[A,B](as: F[A])(z:B)(f: (a,b) => B):B
  def foldLeft[A,B](as: F[A])(z:B)(f: (b,a) => B):B
  def foldMap[A,B](as: F[A])(f: A => B)(mb: Monoid[B]):B
  def contatenate[A](as: F[A])(m: Monoid[A]):A =
    foldLeft(as)(m.zero)(m.op)
}
```

`F[_]` above is a *type constructor* (in this case, one that takes a single argument). `Foldable` is a *higher kinded type*.

Some examples  of `Foldable[F[_]]`: `Foldable[List]`, `Foldable[IndexedSeq]`, `Foldable[Stream]`, `Foldable[Tree]`.

Composing Monoids

Monoids can *compose*, which allows us to build out more complicated functions using more simple functions.

E.g. Product Monoid

    def productMonoid[A,B](ma: Monoid[A])(mb: Monoid[B]):Monoid[(A,B)]

E.g. Map-merge

```
def mapMergeMonoid[K,V](mv: Monoid[V]):Monoid[Map[K,V]] =
  new Monoid[Map[K,V]] {
    def zero = Map[K,V]() //an empty Map
    def op(a: Map[K,V], b: Map[K,V]):Map[K,V] =
      ()(a.keySet) ++ (b.keySet)).foldLeft(zero){ (acc, k) =>
        acc.updated(k, V.op(a.getOrElse(k, V.zero),
                            b.getOrElse(k, V.zero)))
      }
  }
```

Fused traversals using Monoids

Monoids can be used to perform multiple computations over a single traversal.

E.g. keep a running sum of a list of integers and track the number of elements at the same time

```
val m = productMonoid(intAddition, intAddition)

val p = listFoldable.foldMap(List(1,2,3,4))(a => (1,a))(m)

//p: (Int, Int) = (4,10)
```
