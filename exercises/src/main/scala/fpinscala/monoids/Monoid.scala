package fpinscala.monoids

import fpinscala.parallelism.Nonblocking._
import fpinscala.parallelism.Nonblocking.Par.toParOps
import fpinscala.state.RNG.Simple

// infix syntax for `Par.map`, `Par.flatMap`, etc

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}

object Monoid {

  val stringMonoid = new Monoid[String] {
    def op(a1: String, a2: String) = a1 + a2
    val zero = ""
  }

  def listMonoid[A] = new Monoid[List[A]] {
    def op(a1: List[A], a2: List[A]) = a1 ++ a2
    val zero = Nil
  }

  val intAddition: Monoid[Int] = new Monoid[Int]{
    def op(a1: Int, a2:Int) = a1 + a2
    def zero = 0
  }

  val intMultiplication: Monoid[Int] = new Monoid[Int]{
    def op(a1: Int, a2: Int) = a1 * a2
    def zero = 0
  }

  val booleanOr: Monoid[Boolean] = new Monoid[Boolean]{
    def op(b1:Boolean, b2:Boolean) = b1 || b2
    def zero = false
  }

  val booleanAnd: Monoid[Boolean] = new Monoid[Boolean]{
    def op(b1:Boolean, b2:Boolean) = b1 && b2
    def zero = true
  }

  def optionMonoid[A]: Monoid[Option[A]] = new Monoid[Option[A]] {
    def op(a1: Option[A], a2: Option[A]): Option[A] = a1 orElse a2
    def zero: Option[A] = None
  }

  def endoMonoid[A]: Monoid[A => A] = new Monoid[A => A] {
    //compose makes a new function that composes other functions
    //f compose g = f(g(x))
    //vs andThen, which is f andThen g = g(f(x))
    def op(a1: (A) => A, a2: (A) => A): (A) => A = a1 compose a2
    def zero: (A) => A = (a:A) => a
  }

  // We can get the dual of any monoid just by flipping the `op`.
  def dual[A](m: Monoid[A]): Monoid[A] = new Monoid[A] {
    def op(x: A, y: A): A = m.op(y, x)
    val zero = m.zero
  }

  import fpinscala.testing._
  import Prop._
  def monoidLaws[A](m: Monoid[A])(gen: Gen[A]): Prop = {
    val associative:Prop = forAll(
      for {
        x <- gen
        y <- gen
        z <- gen
      } yield(x,y,z)
    ){ case(x,y,z) => m.op(x,m.op(y,z)) == m.op(m.op(x,y),z) }

    val zero:Prop = forAll(gen)(x => m.op(x,m.zero) == x)
    associative && zero
  }

  class CheckableMonoid[A](m:Monoid[A]) {
    def check(implicit g:Gen[A]) = monoidLaws(m)(g)
  }

  object MonoidLaws {
    implicit def toCheckable[A](m:Monoid[A]):CheckableMonoid[A] = new CheckableMonoid(m)
  }

  def trimMonoid(s: String): Monoid[String] = sys.error("todo")

  def concatenate[A](as: List[A], m: Monoid[A]): A =
    as.foldRight(m.zero)(m.op)

  def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B =
    as.foldRight(m.zero)((a,b) => m.op(f(a),b))

  def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B =
    foldMap(as,endoMonoid[B])(f.curried)(z)

  def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B): B =
    foldMap(as,dual(endoMonoid[B]))(a => b => f(b,a))(z)

  def foldMapV[A, B](as: IndexedSeq[A], m: Monoid[B])(f: A => B): B = as match {
    case seq if seq.isEmpty => m.zero
    case seq if seq.length == 1 => f(seq.head)
    case seq  =>
      val split = seq.length/2
      val (l,r) = seq.splitAt(split)
      m.op( foldMapV(l,m)(f), foldMapV(r,m)(f))
  }


  def ordered(ints: IndexedSeq[Int]): Boolean =
    sys.error("todo")

  sealed trait WC
  case class Stub(chars: String) extends WC
  case class Part(lStub: String, words: Int, rStub: String) extends WC

  def par[A](m: Monoid[A]): Monoid[Par[A]] =
    sys.error("todo")

  def parFoldMap[A,B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): Par[B] =
    sys.error("todo")

  val wcMonoid: Monoid[WC] = new Monoid[WC] {
    def op(a1: WC, a2: WC): WC = (a1,a2) match {
      case (Stub(a),Stub(b)) => Stub(a + b)
      case (Stub(a),Part(l,c,r)) => Part(a + l, c, r)
      case (Part(l,c,r),Stub(a)) => Part(l, c, r+ a)
      case (Part(la,ca,ra),Part(lb,cb,rb)) =>
        Part(la, ca + (if ((ra + lb).isEmpty) 0 else 1) + cb, rb)
    }

    def zero: WC = Stub("")
  }

  def count(s: String): Int = {
    def toWc(a:Char):WC =
      if (a.isWhitespace) Part("", 0, "")
      else Stub(a.toString)
    def wcForString(str:String) = if (str.isEmpty) 0 else 1

    foldMapV(s.toIndexedSeq,wcMonoid)(toWc) match {
      case Stub(str) => wcForString(str)
      case Part(l, c, r) => wcForString(l) + c + wcForString(r)
    }
  }

  def productMonoid[A,B](ma: Monoid[A], mb: Monoid[B]): Monoid[(A, B)] = new Monoid[(A, B)] {
    def op(p0: (A, B), p1: (A, B)) = (ma.op(p0._1,p1._1), mb.op(p0._2,p1._2))
    def zero: (A, B) = (ma.zero, mb.zero)
  }

  def functionMonoid[A,B](mb: Monoid[B]): Monoid[A => B] = new Monoid[(A) => B] {
    def op(a1: (A) => B, a2: (A) => B): (A) => B = a => mb.op(a1(a),a2(a))
    def zero: (A) => B = _ => mb.zero
  }

  def mapMergeMonoid[K,V](mv: Monoid[V]): Monoid[Map[K, V]] = new Monoid[Map[K, V]] {
    def op(a1: Map[K, V], a2: Map[K, V]): Map[K, V] = {
      val mergedKeys = a1.keySet ++ a2.keySet
      mergedKeys.foldLeft(zero){(acc,k) =>
        val mergedValue = mv.op(a1.getOrElse(k, mv.zero), a2.getOrElse(k, mv.zero))
        acc.updated(k,mergedValue)
      }
    }

    def zero: Map[K, V] = Map.empty
  }


  def bag[A](as: IndexedSeq[A]): Map[A, Int] =
    foldMapV(as,mapMergeMonoid[A, Int](intAddition))(a => Map(a -> 1))
}

trait Foldable[F[_]] {
  import Monoid._

  def foldRight[A, B](as: F[A])(z: B)(f: (A, B) => B): B =
    foldMap(as)(f.curried)(endoMonoid)(z)

  def foldLeft[A, B](as: F[A])(z: B)(f: (B, A) => B): B =
    foldMap(as)(a => (b:B) => f(b,a))(dual(endoMonoid))(z)

  def foldMap[A, B](as: F[A])(f: A => B)(mb: Monoid[B]): B =
    foldRight(as)(mb.zero)((a,b) => mb.op(f(a),b))

  def concatenate[A](as: F[A])(m: Monoid[A]): A =
    foldRight(as)(m.zero)(m.op)

  def toList[A](as: F[A]): List[A] =
    foldRight(as)(List.empty[A])((a,acc) => a :: acc)
}

object ListFoldable extends Foldable[List] {
  override def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B) =
    as.foldRight(z)(f)
  override def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B) =
    as.foldLeft(z)(f)
  override def foldMap[A, B](as: List[A])(f: A => B)(mb: Monoid[B]): B =
    as.foldLeft(mb.zero)((a,b) => mb.op(a,f(b)))
}

object IndexedSeqFoldable extends Foldable[IndexedSeq] {
  override def foldRight[A, B](as: IndexedSeq[A])(z: B)(f: (A, B) => B) =
    as.foldRight(z)(f)
  override def foldLeft[A, B](as: IndexedSeq[A])(z: B)(f: (B, A) => B) =
    as.foldLeft(z)(f)
  override def foldMap[A, B](as: IndexedSeq[A])(f: A => B)(mb: Monoid[B]): B =
    Monoid.foldMapV(as,mb)(f)
}

object StreamFoldable extends Foldable[Stream] {
  override def foldRight[A, B](as: Stream[A])(z: B)(f: (A, B) => B) =
    as.foldRight(z)(f)
  override def foldLeft[A, B](as: Stream[A])(z: B)(f: (B, A) => B) =
    as.foldLeft(z)(f)
}

sealed trait Tree[+A]
case class Leaf[A](value: A) extends Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

object TreeFoldable extends Foldable[Tree] {
  override def foldMap[A, B](as: Tree[A])(f: A => B)(mb: Monoid[B]): B = as match {
    case Leaf(a) => f(a)
    case Branch(l, r) => mb.op(foldMap(l)(f)(mb), foldMap(r)(f)(mb))
  }

  override def foldLeft[A, B](as: Tree[A])(z: B)(f: (B, A) => B): B = as match {
    case Leaf(a) => f(z, a)
    case Branch(l, r) => foldLeft(l)(foldLeft(r)(z)(f))(f)
  }

  override def foldRight[A, B](as: Tree[A])(z: B)(f: (A, B) => B): B =
    as match {
      case Leaf(a) => f(a, z)
      case Branch(l, r) => foldRight(l)(foldRight(r)(z)(f))(f)
    }
}

object OptionFoldable extends Foldable[Option] {
  override def foldMap[A, B](as: Option[A])(f: A => B)(mb: Monoid[B]): B = as match {
    case Some(a) => f(a)
    case None => mb.zero
  }

  override def foldLeft[A, B](as: Option[A])(z: B)(f: (B, A) => B) = as match {
    case Some(a) => f(z, a)
    case None => z
  }

  override def foldRight[A, B](as: Option[A])(z: B)(f: (A, B) => B) = as match {
    case Some(a) => f(a, z)
    case None => z
  }
}


