package fpinscala
package applicative

import monads.Functor
import state._
import State._
import StateUtil._ // defined at bottom of this file
import monoids._

trait Applicative[F[_]] extends Functor[F] { self =>

  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] = ???

  def apply[A,B](fab: F[A => B])(fa: F[A]): F[B] = ???

  def unit[A](a: => A): F[A]

  def map[A,B](fa: F[A])(f: A => B): F[B] =
    apply[A,B](unit(f))(fa)

  def sequence[A](fas: List[F[A]]): F[List[A]] =
    traverse(fas)(fa => fa)

  def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List[B]()))((a, fbs) => map2(f(a),fbs)(_ :: _))

  def replicateM[A](n: Int, fa: F[A]): F[List[A]] =
    sequence(List.fill(n)(fa))

  //Is this 'product'?
  def factor[A,B](fa: F[A], fb: F[B]): F[(A,B)] =
    map2(fa,fb)((_,_))

  def product[G[_]](G: Applicative[G]): Applicative[({type f[x] = (F[x], G[x])})#f] = new Applicative[({type f[x] = (F[x], G[x])})#f] {
    override def unit[A](a: => A): (F[A], G[A]) = (self.unit(a),G.unit(a))
blagh
    override def apply[A, B](fab: (F[(A) => B], G[(A) => B]))(fa: (F[A], G[A])): (F[B], G[B]) =
      (self.apply(fab._1)(fa._1),G.apply(fab._2)(fa._2))
  }

  def compose[G[_]](G: Applicative[G]): Applicative[({type f[x] = F[G[x]]})#f] =
    new ComposeApplicative(this,G)

  def sequenceMap[K,V](ofa: Map[K,F[V]]): F[Map[K,V]] =
    ofa.foldRight(unit(Map.empty[K,V]))((kv,acc) => map2(kv._2,acc)((a,b) => b + (kv._1 -> a)))

  //Exercise 12.2
  //implement in terms of apply and unit
  def _map[A,B](fa: F[A])(f: A => B): F[B] = apply[A,B](unit(f))(fa)

  def _map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    apply[B,C](_map(fa)(f.curried))(fb)

  //Exercise 12.3
  def map3[A,B,C,D](fa:F[A], fb:F[B], fc:F[C])(f: (A,B,C) => D): F[D] =
    apply(apply(apply[A,B=>C=>D](unit(f.curried))(fa))(fb))(fc)

  def map4[A,B,C,D,E](fa:F[A], fb:F[B], fc:F[C], fd: F[D])(f: (A,B,C,D) => E): F[E] =
    apply(apply(apply(apply[A,B=>C=>D=>E](unit(f.curried))(fa))(fb))(fc))(fd)
}

case class Tree[+A](head: A, tail: List[Tree[A]])


trait Monad[F[_]] extends Applicative[F] {
  def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] = join(map(ma)(f))

  def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)

  def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
    a => flatMap(f(a))(g)

  override def apply[A,B](mf: F[A => B])(ma: F[A]): F[B] =
    flatMap(mf)(f => map(ma)(a => f(a)))
}

object Monad {
  def eitherMonad[E]: Monad[({type f[x] = Either[E, x]})#f] = new Monad[({type f[x] = Either[E, x]})#f] {
    def unit[A](a: => A): Either[E, A] = Right(a)

    override def flatMap[A, B](ma: Either[E, A])(f: (A) => Either[E, B]): Either[E, B] = ma match {
      case Right(a) => f(a)
      case Left(e) => Left(e)
    }

  }

  def stateMonad[S] = new Monad[({type f[x] = State[S, x]})#f] {
    def unit[A](a: => A): State[S, A] = State(s => (a, s))
    override def flatMap[A,B](st: State[S, A])(f: A => State[S, B]): State[S, B] =
      st flatMap f
  }

  def composeM[F[_],N[_]](implicit F: Monad[F], N: Monad[N], T: Traverse[N]):
    Monad[({type f[x] = F[N[x]]})#f] = ???
}

sealed trait Validation[+E, +A]

case class Failure[E](head: E, tail: Vector[E])
  extends Validation[E, Nothing]

case class Success[A](a: A) extends Validation[Nothing, A]

class ProductApplicative[F[_],G[_]](self:Applicative[F],g:Applicative[G]) extends  Applicative[({type f[x] = (F[x], G[x])})#f] {
  override def unit[A](a: => A): (F[A], G[A]) = (self.unit(a),g.unit(a))

  override def apply[A, B](fab: (F[(A) => B], G[(A) => B]))(fa: (F[A], G[A])): (F[B], G[B]) =
    (self.apply(fab._1)(fa._1),g.apply(fab._2)(fa._2))
}

class ComposeApplicative[F[_],G[_]](self:Applicative[F],g:Applicative[G]) extends Applicative[({type f[x] = F[G[x]]})#f] {
  override def unit[A](a: => A): F[G[A]] = self.unit(g.unit(a))

  override def map2[A, B, C](fa: F[G[A]], fb: F[G[B]])(f: (A, B) => C): F[G[C]] =
    self.map2(fa,fb)((ga,gb) => g.map2(ga,gb)(f))
}
object Applicative {
  val streamApplicative = new Applicative[Stream] {

    def unit[A](a: => A): Stream[A] =
      Stream.continually(a) // The infinite, constant stream

    override def map2[A,B,C](a: Stream[A], b: Stream[B])( // Combine elements pointwise
                    f: (A,B) => C): Stream[C] =
      a zip b map f.tupled
  }

  def validationApplicative[E]: Applicative[({type f[x] = Validation[E,x]})#f] = new Applicative[({type f[x] = Validation[E, x]})#f] {
    def unit[A](a: => A): Validation[E, A] = Success(a)

    override def map2[A, B, C](fa: Validation[E, A], fb: Validation[E, B])(f: (A, B) => C): Validation[E, C] = (fa,fb) match {
      case (Success(a),Success(b)) => Success(f(a,b))
      case (Failure(h1,t1),Failure(h2,t2)) => Failure(h1, t1 ++ Vector(h2) ++ t2)
      case (_, e@Failure(_,_)) => e
      case (e@Failure(_,_),_) => e
    }

  }

  type Const[A, B] = A

  implicit def monoidApplicative[M](M: Monoid[M]) =
    new Applicative[({ type f[x] = Const[M, x] })#f] {
      def unit[A](a: => A): M = M.zero
      override def apply[A,B](m1: M)(m2: M): M = M.op(m1, m2)
    }
}

trait Traverse[F[_]] extends Functor[F] with Foldable[F] { self => //always forget that you can do this...
  def traverse[G[_]:Applicative,A,B](fa: F[A])(f: A => G[B]): G[F[B]] =
    sequence(map(fa)(f))
  def sequence[G[_]:Applicative,A](fma: F[G[A]]): G[F[A]] =
    traverse(fma)(ma => ma)

  type Id[A] = A
  val idMonad = new Monad[Id] {
    def unit[A](a: => A) = a
    override def flatMap[A,B](a: A)(f: A => B): B = f(a)
  }

  def map[A,B](fa: F[A])(f: A => B): F[B] =
    traverse[Id, A, B](fa)(f)(idMonad)

  import Applicative._

  override def foldMap[A,B](as: F[A])(f: A => B)(mb: Monoid[B]): B =
    traverse[({type f[x] = Const[B,x]})#f,A,Nothing](
      as)(f)(monoidApplicative(mb))

  def traverseS[S,A,B](fa: F[A])(f: A => State[S, B]): State[S, F[B]] =
    traverse[({type f[x] = State[S,x]})#f,A,B](fa)(f)(Monad.stateMonad)

  def mapAccum[S,A,B](fa: F[A], s: S)(f: (A, S) => (B, S)): (F[B], S) =
    traverseS(fa)((a: A) => (for {
      s1 <- get[S]
      (b, s2) = f(a, s1)
      _  <- set(s2)
    } yield b)).run(s)

  override def toList[A](fa: F[A]): List[A] =
    mapAccum(fa, List[A]())((a, s) => ((), a :: s))._2.reverse

  def zipWithIndex[A](fa: F[A]): F[(A, Int)] =
    mapAccum(fa, 0)((a, s) => ((a, s), s + 1))._1

  def reverse[A](fa: F[A]): F[A] =
    mapAccum(fa,toList(fa).reverse)((_,as) => (as.head, as.tail ))._1

  //Using "unit" as the "B" in mapAccum cos we don't particularly care about it
  override def foldLeft[A,B](fa: F[A])(z: B)(f: (B, A) => B): B =
    mapAccum(fa,z)((a,b) => ((), f(b,a)))._2

  //Not sure where the implicit param on the end comes from
  def fuse[G[_],H[_],A,B](fa: F[A])(f: A => G[B], g: A => H[B])
                         (implicit G: Applicative[G], H: Applicative[H]): (G[F[B]], H[F[B]]) =
    traverse[({type f[x] = (G[x],H[x])})#f,A,B](fa)(a => (f(a),g(a)))(G product H)

  def compose[G[_]](implicit G: Traverse[G]): Traverse[({type f[x] = F[G[x]]})#f] =
    new Traverse[({type f[x] = F[G[x]]})#f] {
      override def traverse[M[_]:Applicative,A,B](fa: F[G[A]])(f: A => M[B]) =
        self.traverse(fa)((ga: G[A]) => G.traverse(ga)(f)) //intellij barfs
    }


  //This implementation of zip will error if fa and fb are different shapes
  def zip[A,B](fa:F[A], fb:F[B]):F[(A,B)] =
    mapAccum(fa, toList(fb)) {
      case (a, Nil) => ???
      case (a, b :: bs) => ((a, b), bs)
    }._1

  //We can have versions of zip that favour either the left or right

  //here, B may or may not have values
  def zipL[A,B](fa:F[A], fb:F[B]):F[(A,Option[B])] =
    mapAccum(fa, toList(fb)) {
      case (a, Nil) => ((a, None), Nil)
      case (a, b :: bs) => ((a, Some(b)), bs)
    }._1

  //here, A may or may not have values
  def zipR[A,B](fa:F[A], fb:F[B]):F[(Option[A],B)] =
    mapAccum(fb, toList(fa)) {
      case (b, Nil) => ((None, b), Nil)
      case (b, a :: as) => ((Some(a), b), as)
    }._1
}


object Traverse {
  val listTraverse = new Traverse[List] {
    override def traverse[G[_], A, B](fa: List[A])(f: (A) => G[B])(implicit g:Applicative[G]): G[List[B]] =
      fa.foldLeft(g.unit(List.empty[B]))((acc,a) => g.map2(f(a),acc)(_ :: _))
  }

  val optionTraverse = new Traverse[Option] {
    override def traverse[G[_],A, B](fa: Option[A])(f: (A) => G[B])(implicit g:Applicative[G]): G[Option[B]] = fa match {
      case Some(a) => g.map(f(a))(b => Some(b))
      case None => g.unit(None)
    }
  }

  val treeTraverse = new Traverse[Tree] {
    override def traverse[G[_], A, B](fa: Tree[A])(f: (A) => G[B])(implicit g:Applicative[G]): G[Tree[B]] =
      g.map2(f(fa.head),listTraverse.traverse(fa.tail)(a => traverse(a)(f)))((a,acc) => Tree(a,acc))
  }
}

// The `get` and `set` functions on `State` are used above,
// but aren't in the `exercises` subproject, so we include
// them here
object StateUtil {

  def get[S]: State[S, S] =
    State(s => (s, s))

  def set[S](s: S): State[S, Unit] =
    State(_ => ((), s))
}
