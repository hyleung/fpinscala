package fpinscala.testing

import fpinscala.laziness.Stream
import fpinscala.state.RNG.Simple
import fpinscala.state._
import fpinscala.parallelism._
import fpinscala.parallelism.Par.Par
import Gen._
import Prop._
import java.util.concurrent.{Executors,ExecutorService}

/*
The library developed in this chapter goes through several iterations. This file is just the
shell, which you can fill in and modify while working through the chapter.
*/


case class Prop(run: (TestCases, RNG) => Result) {
  def &&(p: Prop):Prop = Prop((n,rng) => {
    val r1 = run(n,rng)
    if (r1.isFalsified) r1 else p.run(n,rng)
  })
  def ||(p: Prop):Prop =  Prop((n,rng) => {
    run(n,rng) match {
      case Passed => Passed
      case Falsified(_,_) => p.run(n,rng)
    }
  })
}

object Prop {
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

  def forAll[A](gen: Gen[A])(f: A => Boolean): Prop = Prop(
    (n,rng) => randomStream(gen)(rng).zip(Stream.from(0)).take(n).map {
      case (a, i) => try {
        if (f(a)) Passed else Falsified(a.toString, i)
      } catch {
        case e: Exception => Falsified(buildMsg(a,e), i)
      }
    }.find(_.isFalsified).getOrElse(Passed)
  )

  def randomStream[A](g: Gen[A])(rng: RNG): Stream[A] =
    Stream.unfold(rng)(r => Some(g.sample.run(r)))

  def buildMsg[A](s: A, e:Exception):String =
    s"test case: $s\n" +
    s"generated an exception: ${e.getMessage}\n" +
    s"stack trace:\n ${e.getStackTrace().mkString("\n")}"
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
}

case class Gen[A](sample: State[RNG,A]) {
  def flatMap[B](f: A => Gen[B]): Gen[B] =
    Gen(this.sample.flatMap(a => f(a).sample))
  def listOfN(n:Int):Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(sample)))
}

//trait Gen[A] {
//  def map[A,B](f: A => B): Gen[B] = ???
//  def flatMap[A,B](f: A => Gen[B]): Gen[B] = ???
//}

trait SGen[+A] {

}

