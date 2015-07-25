package fpinscala.laziness

import Stream._
trait Stream[+A] {

  def foldRight[B](z: => B)(f: (A, => B) => B): B = // The arrow `=>` in front of the argument type `B` means that the function `f` takes its second argument by name and may choose not to evaluate it.
    this match {
      case Cons(h,t) => f(h(), t().foldRight(z)(f)) // If `f` doesn't evaluate its second argument, the recursion never occurs.
      case _ => z
    }

  def exists(p: A => Boolean): Boolean = 
    foldRight(false)((a, b) => p(a) || b) // Here `b` is the unevaluated recursive step that folds the tail of the stream. If `p(a)` returns `true`, `b` will never be evaluated and the computation terminates early.

  @annotation.tailrec
  final def find(f: A => Boolean): Option[A] = this match {
    case Empty => None
    case Cons(h, t) => if (f(h())) Some(h()) else t().find(f)
  }
  def take(n: Int): Stream[A] = this match {
    case Cons(h,t) if n > 0 => Cons(h,() => t().take(n-1))
    case Cons(_,_) if n == 0 => Stream.empty[A]
  }

  def drop(n: Int): Stream[A] = this match {
    case Cons(h,t) if n > 0 => t().drop(n -1)
    case Empty if n > 0 => Empty
    case s if n == 0 => s
  }

  def takeWhile(p: A => Boolean): Stream[A] = this match {
    case Cons(h,t) if p(h()) => Cons(h,() => t().takeWhile(p))
    case Cons(h,_) if !p(h()) => Empty
    case Empty => Empty
  }

  def forAll(p: A => Boolean): Boolean = this match {
    case Empty => false
    case _ => this.foldRight(true)((a,b) => p(a) && b)
  }

  def headOption: Option[A] = foldRight(Option.empty[A])((h,t) => Some(h))

  def toList:List[A] = this match {
    case Empty => Nil
    case Cons(h,t) => h() :: t().toList
  }

  // 5.7 map, filter, append, flatmap using foldRight. Part of the exercise is
  // writing your own function signatures.
  def map[B](f: A => B):Stream[B] = this.foldRight(Stream.empty[B])((a,b) => Stream.cons(f(a),b))

  def flatMap[B](f: A => Stream[B]):Stream[B] = foldRight(empty[B])((h,t) => f(h) ++ t)

  def filter(f: A => Boolean):Stream[A] = foldRight(empty[A])((h,t) => if(f(h)) cons(h, t) else t)
  
  def startsWith[B](s: Stream[B]): Boolean = (this,s) match {
    case (_, Empty) => true
    case (Cons(a,aa),Cons(b,bb)) if a().equals(b()) => aa().startsWith(bb())
    case _ => false
  }

  def ++[B>:A](s: => Stream[B]):Stream[B] = foldRight(s)((h,t) =>  cons(h,t))

  def mapWithUnfold[B](f: A => B):Stream[B] = unfold(this){
    case Empty => None
    case Cons(h,t) => Some(f(h()),t())}

  def takeUnfold(n: Int): Stream[A] = unfold((n,this)){
    case (i,Cons(h,t)) if i > 1 => Some((h(),(i - 1, t())))
    case (1,Cons(h,_))  => Some((h(),(0, Stream.empty)))
    case _ => None
  }
  def takeWhileUnfold(p: A => Boolean): Stream[A] = unfold(this){
    case Empty => None
    case Cons(h,t) if p(h()) => Some(h(),t())
    case Cons(h,_) if !p(h()) => None
  }

  def zip[B](s2: Stream[B]): Stream[(A,B)] = (this,s2) match {
    case (_, Empty) => Empty
    case (Empty, _) => Empty
    case (Cons(a,aa), Cons(b,bb)) => cons((a(),b()), aa().zip(bb()) )
  }

  def zipAll[B](s2: Stream[B]): Stream[(Option[A],Option[B])] = (this,s2) match {
    case (Empty,Empty) => empty
    case (Cons(a,aa), Empty) => cons((Some(a()), None), aa().zipAll(Empty))
    case (Empty, Cons(b,bb)) => cons((None, Some(b())), Empty.zipAll(bb()))
    case (Cons(a,aa), Cons(b,bb)) => cons((Some(a()),Some(b())), aa().zipAll(bb()) )
  }

  def tails: Stream[Stream[A]] = this match {
    case Empty => Stream(empty)
    case Cons(a,aa) => Cons(() => this, () => aa().tails)
  }

  def tailsUnfold: Stream[Stream[A]] = unfold(this){
    case Empty => None
    case s => Some((s, s drop 1))
   } ++ Stream(empty)
}
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

object Stream {
  def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }

  def empty[A]: Stream[A] = Empty

  def apply[A](as: A*): Stream[A] =
    if (as.isEmpty) empty 
    else cons(as.head, apply(as.tail: _*))

  val ones: Stream[Int] = constant(1)
  def from(n: Int): Stream[Int] = unfold(n)(s => Some(s,s + 1))

  def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A] = f(z) match {
    case Some((v, next)) => cons(v, unfold(next)(f))
    case None => Stream.empty[A]
  }

  def constant[A](a: A): Stream[A] = unfold(a)(_ => Some(a,a))

  //0, 1, 1, 2, 3, 5, 8,
  //def fibs:Stream[Int] = unfold((0,1))(s => Some(s._1, (s._2, s._1 + s._2)))
  def fibs:Stream[Int] = unfold((0,1)){ case (f1,f2) => Some(f1, (f2, f1 + f2))}

}