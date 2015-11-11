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
