package fpinscala.datastructures

sealed trait List[+A]

// `List` data type, parameterized on a type, `A`
case object Nil extends List[Nothing]

// A `List` data constructor representing the empty list
/* Another data constructor, representing nonempty lists. Note that `tail` is another `List[A]`,
which may be `Nil` or another `Cons`.
 */
case class Cons[+A](head: A, tail: List[A]) extends List[A]

object List {
	// `List` companion object. Contains functions for creating and working with lists.
	def sum(ints: List[Int]): Int = ints match {
		// A function that uses pattern matching to add up a list of integers
		case Nil => 0 // The sum of the empty list is 0.
		case Cons(x, xs) => x + sum(xs) // The sum of a list starting with `x` is `x` plus the sum of the rest of the list.
	}

	def product(ds: List[Double]): Double = ds match {
		case Nil => 1.0
		case Cons(0.0, _) => 0.0
		case Cons(x, xs) => x * product(xs)
	}

	def apply[A](as: A*): List[A] = // Variadic function syntax
		if (as.isEmpty) Nil
		else Cons(as.head, apply(as.tail: _*))

	val x = List(1, 2, 3, 4, 5) match {
		case Cons(x, Cons(2, Cons(4, _))) => x
		case Nil => 42
		case Cons(x, Cons(y, Cons(3, Cons(4, _)))) => x + y
		case Cons(h, t) => h + sum(t)
		case _ => 101
	}

	def append[A](a1: List[A], a2: List[A]): List[A] =
		a1 match {
			case Nil => a2
			case Cons(h, t) => Cons(h, append(t, a2))
		}

	def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B = // Utility functions
		as match {
			case Nil => z
			case Cons(x, xs) => f(x, foldRight(xs, z)(f))
		}

	def sum2(ns: List[Int]) =
		foldRight(ns, 0)((x, y) => x + y)

	def product2(ns: List[Double]) =
		foldRight(ns, 1.0)(_ * _) // `_ * _` is more concise notation for `(x,y) => x * y`; see sidebar


	def tail[A](l: List[A]): List[A] = l match {
		case (Cons(h, t)) => t
		case Nil => List()
	}


	def setHead[A](l: List[A], h: A): List[A] = l match {
		case Cons(_, t) => Cons(h, t)
		case Nil => sys.error("empty list")
	}

	@annotation.tailrec
	def drop[A](l: List[A], n: Int): List[A] = {
		if (n <= 0) l
		else l match {
			case Nil => List()
			case Cons(h, t) => drop(t, n - 1)
		}
	}

	@annotation.tailrec
	def dropWhile[A](l: List[A], f: A => Boolean): List[A] = {
		l match {
			case Nil => List()
			case Cons(h, t) => if (f(h)) dropWhile(t, f) else l
		}
	}

	def init[A](l: List[A]): List[A] = l match {
		case Nil => List()
		case Cons(_, Nil) => List()
		case Cons(h, t) => Cons(h, init(t))
	}

	def length[A](l: List[A]): Int = foldLeft(l, 0)((a, _) => a + 1)

	def lengthFoldRight[A](l: List[A]): Int = foldRight(l, 0)((a, b) => b + 1)

	@annotation.tailrec
	def foldLeft[A, B](l: List[A], z: B)(f: (B, A) => B): B = l match {
		case Nil => z
		case Cons(h, t) => foldLeft(t, f(z, h))(f)
	}


	def map[A, B](l: List[A])(f: A => B): List[B] = l match {
		case Nil => List()
		case Cons(h, t) => Cons(f(h), map(t)(f))
	}

	def reverse[A](l: List[A]): List[A] = foldLeft(l, List[A]())((acc, b) => Cons(b, acc))

	def foldRightWithLeft[A, B](as: List[A], z: B)(f: (A, B) => B): B =
		foldLeft(reverse(as), z)((b, a) => f(a, b))

	def append2[A](a1: List[A], a2: List[A]): List[A] = foldRight(a1,a2)((a,b) => Cons(a,b))

	def concat[A](l: List[List[A]]): List[A] = foldLeft(l,List[A]())((acc,next) => append(acc, next))

	def add1(l: List[Int]): List[Int] = map(l)(_ + 1)

	def filter[A](l: List[A])(f: A => Boolean): List[A] =
		foldRight(l, List[A]())((a, acc) => if (f(a)) acc else Cons(a, acc))

	def flatMap[A,B](l: List[A])(f: A => List[B]): List[B] =
		concat(foldRight(l,List[List[B]]())((a,acc) => Cons(f(a),acc)))

	def addPairwise(as: List[Int], bs: List[Int]): List[Int] = zipWith(as, bs)(_ + _)

	def zipWith[A,B,C](as: List[A], bs: List[B])(f: (A,B) => C): List[C] = (as, bs) match {
		case (Nil, _) => List()
		case (_, Nil) => List()
		case (Cons(ah, at), Cons(bh, bt)) => Cons(f(ah, bh), zipWith(at, bt)(f))
	}

	@annotation.tailrec
	def startsWith[A](l: List[A], prefix: List[A]): Boolean = (l, prefix) match {
		case (_,Nil) => true
		case (Cons(lh, lt),Cons(ph, pt)) if ph == lh => startsWith(lt, pt)
		case _ => false
 	}

	@annotation.tailrec
	def hasSubsequence[A](sup: List[A], sub: List[A]): Boolean = sup match {
		case Nil if sub == Nil => true
		case Nil => false
		case _ if startsWith(sup, sub) => true
		case Cons(h,t) => hasSubsequence(t,sub)
	}

}
