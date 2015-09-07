package fpinscala.testing

import fpinscala.laziness.Stream
import fpinscala.state._
import fpinscala.testing.Gen._
import fpinscala.testing.Prop._

/*
The library developed in this chapter goes through several iterations. This file is just the
shell, which you can fill in and modify while working through the chapter.
*/


case class Prop(run: (MaxSize, TestCases, RNG) => Result) {
  def &&(p: Prop) = Prop((max, n,rng) => {
    run(max,n,rng) match {
      case Proved | Passed => p.run(max,n, rng)
      case x => x
    }
  })

  def ||(p: Prop):Prop =  Prop((max, n,rng) => {
    run(max, n,rng) match {
      case Falsified(_,_) => p.run(max, n,rng)
      case p => p
    }
  })
}

object Prop {
  type MaxSize = Int
  type TestCases = Int
  type SuccessCount = Int
  type FailedCase = String

  sealed trait Result {
    def isFalsified: Boolean
  }

  case object Passed extends Result {
    override def isFalsified: Boolean = false
  }

  case class Falsified(failure: FailedCase, successes: SuccessCount) extends Result {
    override def isFalsified: Boolean = true
  }

  case object Proved extends Result {
    override def isFalsified: Boolean = false
  }

  def forAll[A](as: Gen[A])(f: A => Boolean): Prop = Prop {
    (n,rng) => randomStream(as)(rng).zip(Stream.from(0)).take(n).map {
      case (a, i) => try {
        if (f(a)) Passed else Falsified(a.toString, i)
      } catch { case e: Exception => Falsified(buildMsg(a, e), i) }
    }.find(_.isFalsified).getOrElse(Passed)
  }

  def forAll[A](g: SGen[A])(f: A => Boolean): Prop = forAll(g(_))(f)

  def forAll[A](g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
    (max,n,rng) =>
      val casesPerSize = (n - 1) / max + 1
      val props: Stream[Prop] =
        Stream.from(0).take((n min max) + 1).map(i => forAll(g(i))(f))
      val prop: Prop =
        props.map(p => Prop { (max, n, rng) =>
          p.run(max, casesPerSize, rng)
        }).toList.reduce(_ && _)
      prop.run(max,n,rng)
  }

  def randomStream[A](g: Gen[A])(rng: RNG): Stream[A] =
    Stream.unfold(rng)(r => Some(g.sample.run(r)))

  def buildMsg[A](s: A, e:Exception):String =
    s"test case: $s\n" +
    s"generated an exception: ${e.getMessage}\n" +
    s"stack trace:\n ${e.getStackTrace().mkString("\n")}"

  def apply(f: (TestCases,RNG) => Result): Prop =
    Prop { (_,n,rng) => f(n,rng) }

  def run(p: Prop,
             maxSize: Int = 100,
             testCases: Int = 100,
             rng: RNG = RNG.Simple(System.currentTimeMillis())) : Unit = {
    p.run(maxSize, testCases, rng) match {
      case Falsified(failure, successCount) =>
            throw new AssertionError(s"! Falsified after $successCount passed")
      case Passed =>
            println(s"OK, passed $testCases tests")
      case Proved =>
            println(s"OK, proved property")
    }
  }

  def check (p: => Boolean): Prop = {
    lazy val result = p
    forAll(unit(()))(_ => result)
  }
}

object Gen {
  def choose(start: Int, stopExclusive: Int):Gen[Int] =
    Gen(State(RNG.nonNegativeInt).map(n => start + n % (stopExclusive - start)))
  def unit[A](a: => A): Gen[A] = Gen(State.unit(a))
  def boolean:Gen[Boolean] =
    Gen(State(RNG.int).map(i => if (i % 2==0) true else false))
  def sameParity(from: Int, to: Int): Gen[(Int,Int)] =
    choose(from, to).listOfN(2).flatMap(l => unit(l.head,l.drop(1).head))
  def union[A](g1: Gen[A], g2:Gen[A]):Gen[A] = boolean.flatMap( b => if (b) g1 else g2)
  def weighted[A](g1: (Gen[A],Double), g2: (Gen[A],Double)):Gen[A] =
    Gen(State(RNG.double)
        .map{ d => if (d <= g1._2) g1 else g2})
        .flatMap( g => g._1)
  def listOf[A](g: Gen[A]):SGen[List[A]] = SGen((n) => g.listOfN(n))
  def listOf1[A](g: Gen[A]):SGen[List[A]] = SGen((n) => g.listOfN(n max 1)) // n or 1
}

case class Gen[+A](sample: State[RNG,A]) {
  def flatMap[B](f: A => Gen[B]): Gen[B] =
    Gen(this.sample.flatMap(a => f(a).sample))
  def listOfN(n:Int):Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(sample)))
  def unsized = SGen( _ => this)
}

//trait Gen[A] {
//  def map[A,B](f: A => B): Gen[B] = ???
//  def flatMap[A,B](f: A => Gen[B]): Gen[B] = ???
//}


case class SGen[+A](forSize: Int => Gen[A]) {
  def apply(n: Int): Gen[A] = forSize(n)
}

object SGen {

}

//trait SGen[+A] {
//
//}



