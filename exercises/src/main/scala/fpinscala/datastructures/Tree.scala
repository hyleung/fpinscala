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
    case Branch(l,r) => if (maximum(l) >= maximum(r)) maximum(l) else maximum(r)
  }
  def depth(t: Tree[Int]):Int = t match {
    case Leaf(_) => 0
    case Branch(l,r) =>  1 + (depth(l) max depth(r))
  }
  def map[A,B](t: Tree[A])(f: A => B): Tree[B] = t match {
    case Leaf(x) => Leaf(f(x))
    case Branch(l,r) => Branch(map(l)(f), map(r)(f))
  }
  def fold[A,B](t: Tree[A])(l: A => B)(b: (B,B) => B): B = t match {
    case Leaf(x) => l(x)
    case Branch(left,right) => b(fold(left)(l)(b), fold(right)(l)(b))
  }
}