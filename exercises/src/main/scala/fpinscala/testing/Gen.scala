package fpinscala.testing

import fpinscala.laziness.Stream
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

trait Prop {
  def check:Either[(FailedCase, SuccessCount), SuccessCount]
}

object Prop {
  type SuccessCount = Int
  type FailedCase = String
  def forAll[A](gen: Gen[A])(f: A => Boolean): Prop = ???
}

object Gen {
  def choose(start: Int, stopExclusive: Int):Gen[Int] =
    Gen(State(RNG.nonNegativeInt).map(n => start + n % (stopExclusive - start)))
  def unit[A](a: => A): Gen[A] = Gen(State.unit(a))
  def boolean:Gen[Boolean] =
    Gen(State(RNG.int).map(i => if (i % 2==0) true else false))
  def sameParity(from: Int, to: Int): Gen[(Int,Int)] =
    choose(from, to).listOfN(2).flatMap(l => unit(l.head,l.drop(1).head))
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

