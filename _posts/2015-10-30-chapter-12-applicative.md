---
layout: post
title:  "Chapter 12: Applicative and Traversable Functors"
date:   2015-10-49 20:32:40 -0700
categories: fpinscala chapter_notes
---
## Applicative and Traversable Functors

Consider `sequence` and `traverse`:

```
def sequence[A](lfa: List[F[A]]):F[List[A]] =
  traverse(lfa)(fa => fa)

def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List[B]()))((a,acc) => map2(f(a),acc)(_ :: :))
```

For a Monad based on `flatMap` and `unit`, `map2` would be implemented in terms of `flatMap`. Notice, however, that `traverse` doesn't call `flatMap` directly. If we had an implementation of `map2` and `unit`, we'd be able to use implement `traverse` (and, therefore, `sequence`) **without** `flatMap`.

If we let `unit` and `map2` be the primitives, we get a new abstraction, the **Applicative Functor**.

### Expressed as a Scala trait

```
trait Applicative[F[_]] extends Functor[F] {
  //primitives
  def map2[A,B,C](fa:F[A], fb:F[B])(f: (A,B) => C):F[C]
  def unit[A](a: => A):F[A]
  //derived
  def map[A,B](fa:F[A])(f: A => B):F[B] =
    map2(fa,unit(()))((a,_) => f(a))
  def traverse[A,B](as: List[A])(unit(List[B]()))((a,acc) => map2(f(a),acc)(_ :: _))
  …  
}
```
**all applicatives are functors** (since we can derive `map` via `map2` and `unit`).

We can provide implementations of `sequence`, `replicateM`, `product`, etc. using only `unit` and `map2`.

```
def sequence[A](fas:List[F[A]):F[List[A]]
def replicateM[A](n:Int,fa:F[A]):F[List[A]]
def product[A,B](fa:F[A], fb:F[B]):F[(A,B)]
```

### Applicative in terms of `unit` and `apply`

The name *applicative* comes from the fact that we can implement `Applicative` via an alternate set of primitives:

```
trait Applicative[F[_]] extends Functor[F] {
  //primitives
  def apply[A,B](fab: F[A => B])(fa:F[A]):F[B]
  def unit[A](a: => A):F[A]
  //derived
  def map[A,B](fa:F[A])(f: A => B):F[B] =
    apply[A,B](unit(f))(fa)
  def map2[A,B,C](fa:F[A],fb:F[B])(f:(A,B) => C):F[C] =
    apply[B,C](map(fa)(f.curried))(fb)
}
```
