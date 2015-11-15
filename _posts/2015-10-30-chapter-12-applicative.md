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

### `Monad[F]` as a subtype of `Applicative[F]`

If we implement `map2` using `flatMap`, we can make `Monad` a subtype of `Applicative`. I.e. *all monads are applicative functors*.

```
trait Monad[F[_]] extends Applicative[F] {
  def flatMap[A,B](fa:F[A])(f:A => F[B]):F[B] =
    join(map(fa)(f))

  def join[A](ffa:F[F[A]]):F[A] =
    flatMap(ffa)(fa => fa)

  def compose[A,B,C](f: A => F[B], g: B => F[C]):A => F[C] =
    a => flatMap(f(a))(g)

  def map[B](fa:F[A])(f: A => B):F[B] =
    flatMap(fa)(a => unit(f(a)))

  def map2[A,B,C](fa:F[A], fb:F[B])(f: (A,B) => C):F[C] =
    flatMap(fa)(a => map(fb)(b => f(a,b)))
}
```

A minimal Monad implementation will need to implement `unit` and override either `flatMap` or `join` and `map`. We get `Applicative` "for free".

### "Effects" in FP

The type constructors (that's the `F[_]` part) - like `Par`, `Gen`, `List`, `Option`, etc. - are sometimes called "effects" (not to be confused with *side effects*). This is because they add extra capabilities onto ordinary values. For example, `Option` adds the idea that there may be some value or there may be not.

In the context of Monad, Applicative, etc. - we can say that types that have associated Monad or Applicative instances have *monadic effects*, *applicative effects*.

### Applicative vs Monad

- Applicative is good for sequencing *effects*, computations with *fixed structure*
- Monad computations can change their structure dynamically based on the results of previous effects
- Applicative work for *context-free* computations, whereas Monad works for *context sensitive* computations (where we need to know about previous effects)
- Applicative functors compose, whereas Monads (in general) do not

Applicative is more general - therefore more common. Functions like `traverse` are preferably implemented using the primitives from Applicative (i.e. `map2` and `unit`) - make fewer assumptions. This way we can, for example, get `traverse` "for free" in more cases.

### Applicative laws

#### Identity

#### Left Identity

    map2(unit(()), fa)((_,a) => a) == fa

#### Right Identity

    map2(fa, unit(()))((a,_) => a) == fa

#### Associativity

Stated in terms of product and assoc:

    def product[A,B](fa:F[A], fb:F[B]):F[(A,B)] =
      map2(fa, fb)((_,_))

    //function to go from right-nested to left-nested
    def assoc[A,B,C](p:(A,(B,C))):((A,B),C) =
      p match { case (a,(b,c)) => ((a,b),c)}  

Then:

    product(product(fa,fb),fc) == map(product(fa, product(fb,fc))(assoc)

#### Naturality of product

*"When working with Applicative effects, we generally have the option of applying transformations before or after combining values with `map2`"*

"Naturality" - states that it doesn't matter, we get the same result either way.

    map2(fa,fb)((a,b) => (f(a),g(b)) == product(map(fa)(f),map(fb)(g))

`f` and `g` are the transformations.

**[Note: In the book, a function `def productF[I, O, I2, O2](f: I => O, g: I2 => 02):(I,I2) => (O,O2)]` is used.**    
