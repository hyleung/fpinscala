---
layout: post
title:  "Chapter 11: Monads"
date:   2015-10-29 20:32:40 -0700
categories: fpinscala chapter_notes
---

## Functors

"…a [functor](https://en.wikipedia.org/wiki/Functor) is type of mapping between categories…Functors can be thought of as *homomorphisms* between categories."

Represented as a Scala trait:

```
trait Functor[F[_]] {
  def map[A,B](as:F[A])(f:a => b):F[B]
}
```

Again, this a a higher-kinded type, where `F[_]` could be a `List`, `Option`, etc.

What can we do with this Functor?

...implement a generic "unzip" function:

```
def distribute[A,B](fab:F[(A,B)]):(F[A],F[B]) =
  (map(fab)(_._1), map(fab)(_._2))
```

### Functor laws

    map(x)(a => a) = x

"…mapping over a structure x with the identity function should itself be an identity."

`map(x)` preserves the structure of `x` - this means we can't throw exceptions, remove elements, re-order elements, etc.

## Monads

*"A monad is an implementation of one of the minimal sets of monadic combinators, satisfying the laws of associativity and identity"*

Instances of `Monad` will need to provide implementations for one of these minimal sets of primitive combinators:

- `unit` and `flatMap`
- `unit` and `compose`
- `unit`, `map`, and `join`

…where:

    def unit[A](a: => A):F[A]

    def flatMap[A,B](fa: F[A](f: A => F[B]):F[B]

    def compose[A,B,C](f: A => F[B], g: B => F[C]):A => F[C]

    def join[A](ffa: F[F[A]]):F[A]

    def map[A,B](fa: F[A])(f: A => B):F[B]

`unit` is sometimes referred to as `return`. `flatMap` is sometimes referred to as `bind`.

Expressed as a Scala trait (recall that `map` is defined on `Functor`):

```
trait Monad[M[_]] extends Functor[M] {
  def unit[A](a => A):M[A]
  def flatMap[A,B](ma: M[A])(f: A => M[B]):M[B]
}
```

### Monad laws

#### Associativity

For `unit` and `flatMap`, this law can be expressed:

    x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))

A clearer view using `compose` - using *monadic functions* like `A => F[B]`. These function types are called *Kleisli arrows* - which can be composed.

    compose(compose(f,g),h) == compose(f,compose(g,h))

…which looks more like the formulation of the associativity law for `Monoid`.

#### Identity

Expressed in terms of `flatMap`:

    flatMap(x)(unit) == x

    flatMap(unit(y))(f) == f(y)

Expressed in terms of `compose`:

    compose(f,unit) == f

    compose(unit,f) == f

### Monadic combinators

Based on the minimal set of primitive combinators, we can define a number of useful functions, including:

    def map2[A,B,C](ma: M[A], mb: M[B])(f: (A,B) => C):M[C]

    def sequence[A](lma: List[M[A]]):M[List[A]]

    def traverse[A,B](la: List[A])(f: A => M[B]):M[List[B]]

    def replicateM[A](n: Int, ma: M[A]):M[List[A]]

    def product[A,B](ma: M[A], mb: M[B]):M[(A,B)]

    def filterM[A](ms: List[A])(f: A => M[Boolean]):M[List[A]]

…plus `map`, `join`, `compose`, etc. (depending on which set of primitives we chose).

### Monad Examples

#### Identity Monad

```
case class Id[A](value: A) {
  def map[B](f: A => B): Id[B] = Id(f(value))
  def flatMap[B](f: A => Id[B]): Id[B] = f(value)
}
```    

#### State Monad and Partial type application

Recall our `State` data type from chapter 6:

```
case class State[S,+A](run: S => (A, S)) {
  ...
}
```

We'd like implement a Monad instance for this type, but the type constructor takes *two* arguments.

So...we can define a type with one of the arguments fixed. For example, where `S` is `Int`:

```
type IntState[A] = State[Int,A]
```

Then we can define our `IntState` Monad as follows:

```
object IntStateMonad extends Monad[IntState] {
  ...
}
```

Another way to accomplish this is to use an anonymous type:

```
object IntStateMonad extends Monad[({type IntState[A] = State[Int,A]})#IntState] {
  ...
}
```

When the type constructor is declared inline like this, it is sometimes referred to as a *type lambda* in Scala.

We can use this approach to *partially apply* the type constructor as follows:

```
def stateMonad[S] = new Monad[({type f[x] = State[S,x]})#f] {
  def unit[A](a: => A):State[S,A] = State(s => (a,s))
  def flatMap[A,B](state: State[S,A])(fa: A => State[S,B]):State[S,B] =
    state flatMap fa
}
```
