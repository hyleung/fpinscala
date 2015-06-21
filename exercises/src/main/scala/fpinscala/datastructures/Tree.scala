package fpinscala.datastructures

sealed trait Tree[+A]
case class Leaf[A](value: A) extends Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]


object Tree {
  def size[A](t: Tree[A]): Int = t match {
    case Leaf(_) => 1
    case Branch(l,r) => size(l) + size(r)
  }
  def maximum(t: Tree[Int]): Int = t match {
    case Leaf(x) => x
    case Branch(l,r) => maximum(l) max maximum(r)
  }

  def maximumFold(t: Tree[Int]): Int = fold(t)((a:Int) => a)((l,r) =>  l max r)

  def depth(t: Tree[Int]):Int = t match {
    case Leaf(_) => 0
    case Branch(l,r) =>  1 + (depth(l) max depth(r))
  }

  def depthFold(t: Tree[Int]):Int = fold(t)((_) => 0)((l,r) => 1 + (l max r))

  def map[A,B](t: Tree[A])(f: A => B): Tree[B] = t match {
    case Leaf(x) => Leaf(f(x))
    case Branch(l,r) => Branch(map(l)(f), map(r)(f))
  }

  def mapFold[A,B](t: Tree[A])(f: A => B): Tree[B] = fold(t)((a) => Leaf(f(a)):Tree[B])((l,r) => Branch(l,r))

  def fold[F,G](t: Tree[F])(l: F => G)(b: (G,G) => G): G = t match {
    case Leaf(x) => l(x)
    case Branch(left,right) => b(fold(left)(l)(b), fold(right)(l)(b))
  }
}