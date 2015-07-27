package fpinscala.laziness

import org.scalatest.{FlatSpec, Matchers}
import Stream._
/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-05
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
class StreamSpec extends FlatSpec with Matchers{

	behavior of "Stream.take"
	it should "take n values" in {
		ones.take(5).toList should be (List(1,1,1,1,1))
	}
	it should "take 1 value" in {
		val result = ones.take(1)
		result.toList should be (List(1))
	}
	it should "take 0 values" in {
		val result = ones.take(0)
		result.toList should be (Nil)
	}
	behavior of "Stream.takeViaUnfold"
	it should "take n values" in {
		ones.takeUnfold(5).toList should be (List(1,1,1,1,1))
	}
	it should "take 1 value" in {
		val result = ones.takeUnfold(1)
		result.toList should be (List(1))
	}
	it should "take 0 values" in {
		val result = ones.takeUnfold(0)
		result.toList should be (Nil)
	}
	behavior of "Stream.toList"
	it should "convert a stream to a list" in {
		Stream(1,2,3,4).toList should be (List(1,2,3,4))
	}
	behavior of "Stream.drop"
	it should "drop n values" in {
		Stream(1,2,3,4,5).drop(3).toList should be (List(4,5))
	}
	it should "drop 0 values" in {
		Stream(1,2,3,4,5).drop(0).toList should be (List(1,2,3,4,5))
	}
	it should "drop only up to the length values" in {
		Stream(1,2,3,4,5).drop(10).toList should be (Nil)
	}
	behavior of "Stream.takeWhile"
	it should "take elements until predicate is false" in {
		val s = Stream(1,2,3,4,5,6,7,8,9,10).takeWhile(i => i <=5)
		s.toList should be (List(1,2,3,4,5))
	}
	it should "return empty if stream is empty" in {
		Stream.empty[Int].takeWhile(i => i <=5) should be (Stream.empty)
	}
	it should "return empty if first element fails predicate" in {
		Stream(1,2,3,4,5,6,7,8,9,10).takeWhile(i => i < 1) should be (Stream.empty)
	}

	behavior of "Stream.takeWhileUnfold"
	it should "take elements until predicate is false" in {
		val s = Stream(1,2,3,4,5,6,7,8,9,10).takeWhileUnfold(i => i <=5)
		s.toList should be (List(1,2,3,4,5))
	}
	it should "return empty if stream is empty" in {
		Stream.empty[Int].takeWhileUnfold(i => i <=5) should be (Stream.empty)
	}
	it should "return empty if first element fails predicate" in {
		Stream(1,2,3,4,5,6,7,8,9,10).takeWhileUnfold(i => i < 1) should be (Stream.empty)
	}

	behavior of "Stream.forAll"
	it should "return true if all elements in a Stream match a predicate" in {
		val s = Stream(1,2,3,4,5,6,7,8,9,10)
		s.forAll{i => i<=10} should be (true)
	}
	it should "return false if any element doesn't match the predicate" in {
		val s = Stream(1,2,3,4,5,6,7,8,9,10)
		s.forAll{i => i!=5} should be (false)
	}
	it should "return false for an empty stream" in {
		Stream.empty[Int].forAll{_ => true} should be (false)
	}
	behavior of "Stream.map"
	it should "map a function over all elements in a Stream" in {
		ones.map{i => i * 2}.take(5).toList should be (List(2,2,2,2,2))
	}
	it should "return empty if applied to empty Stream" in {
		Stream.empty[Int].map{_ *2} should be (Stream.empty)
	}
	behavior of "Stream.mapWithUnfold"
	it should "map a function over all elements in a Stream" in {
		ones.mapWithUnfold{i => i * 2}.take(5).toList should be (List(2,2,2,2,2))
	}
	it should "return empty if applied to empty Stream" in {
		Stream.empty[Int].mapWithUnfold{_ *2} should be (Stream.empty)
	}
	behavior of "Stream.append"
	it should "append two streams" in {
		(Stream(1,2) ++ Stream(3,4)).toList should be (List(1,2,3,4))
	}
	it should "append stream onto empty" in {
		(Stream.empty[Int] ++ Stream(1,2,3)).toList should be (List(1,2,3))
	}
	it should "append empty onto stream" in {
		(Stream(1,2,3)++ Stream.empty).toList should be (List(1,2,3))
	}
	behavior of "Stream.flatMap"
	val twos:Stream[Int] = Stream.cons(2,twos)
	val threes:Stream[Int] = Stream.cons(3,threes)
	it should "flatMap" in {
		ones.flatMap(a => Stream(a)).take(5).toList should be (List(1,1,1,1,1))
	}
	it should "flatMap/map" in {
		val result = twos.flatMap{a => threes map { b =>  a + b} }
		result.take(5).toList should be (List(5,5,5,5,5))
	}
	it should "return empty when applied to empty" in {
		Stream.empty[Int].flatMap(a => Stream(a)) should be (Stream.empty)
	}
	behavior of "Stream.filter"
	it should "filter out elements not matching a predicate" in {
		Stream(1,2,3,4,5).filter(_ % 2 == 0).toList should be (List(2,4))
	}
	it should "return empty if applied to empty" in {
		Stream.empty[Int].filter(_ % 2 == 0) should be (Stream.empty)
	}
	behavior of "Stream.from"
	it should "construct stream of integers from..." in {
		Stream.from(1).take(5).toList should be (List(1,2,3,4,5))
	}
	behavior of "Stream.headOption"
	it should "return Some if exists" in {
		Stream(1,2,3,4,5).headOption should be (Some(1))
	}
	it should "return None if no elements exist" in {
		Stream.empty[Int].headOption should be (None)
	}
	behavior of "Stream.constant"
	it should "return stream of constant value" in {
		Stream.constant(1).take(5).toList should be (List(1,1,1,1,1))
	}
	behavior of "Stream.unfold"
	it should "unfold to some" in {
		val s = Stream.unfold(0)(s => if (s < 10) Some(s, s + 1) else None)
		s.toList should be (List(0,1,2,3,4,5,6,7,8,9))
	}
	it should "unfold to empty" in {
		val s = Stream.unfold(0)(_ => None)
		s should be (Stream.empty[Int])
	}
	behavior of "Stream.fibs"
	it should "eval to a fib stream" in {
		fibs.take(7).toList should be (List(0, 1, 1, 2, 3, 5, 8))
	}

	behavior of "Stream.zip"
	it should "zip two streams of equal length" in {
		val s1 = Stream(1,2,3)
		val s2 = Stream(1,2,3)
		s1.zip(s2).toList should be (List((1,1),(2,2),(3,3)))
	}
	it should "zip two streams of unequal length" in {
		val s1 = Stream(1,2,3)
		val s2 = Stream(1,2)
		s1.zip(s2).toList should be (List((1,1),(2,2)))
	}
	it should "return empty Stream if param is empty" in {
		val s1 = Stream(1,2,3)
		s1.zip(Stream.empty) should be (Stream.empty)
	}
	it should "return empty Stream if stream is empty" in {
		val s2 = Stream(1,2,3)
		Stream.empty.zip(s2) should be (Stream.empty)
	}
	it should "zip two empty streams" in {
		val s1 = Stream.empty
		val s2 = Stream.empty
		s1.zip(s2) should be (Stream.empty)
	}

	behavior of "Stream.zipAll"
	it should "zipAll two streams of equal length" in {
		val s1 = Stream(1,2,3)
		val s2 = Stream(1,2,3)
		s1.zipAll(s2).toList should be (List(
			(Some(1), Some(1)),
			(Some(2), Some(2)),
			(Some(3), Some(3))))
	}
	it should "zipAll two streams of unequal length" in {
		val s1 = Stream(1,2,3)
		val s2 = Stream(1,2)
		s1.zipAll(s2).toList should be (List(
			(Some(1), Some(1)),
			(Some(2), Some(2)),
			(Some(3), None)))
	}
	it should "return empty Stream if param is empty" in {
		val s1 = Stream(1,2,3)
		s1.zipAll(Stream.empty).toList should be (List(
				(Some(1),None),
				(Some(2),None),
				(Some(3),None)
		))
	}
	it should "return empty Stream if stream is empty" in {
		val s2 = Stream(1,2,3)
		Stream.empty.zipAll(s2).toList should be (List(
			(None, Some(1)),
			(None, Some(2)),
			(None, Some(3))))
	}
	it should "zipAll two empty streams" in {
		val s1 = Stream.empty
		val s2 = Stream.empty
		s1.zipAll(s2) should be (Stream.empty)
	}
	behavior of "Stream.startsWith"
	it should "evaluate to true if stream starts with arg" in {
		val s1 = Stream(1,2,3,4,5)
		val s2 = Stream(1,2,3)
		s1.startsWith(s2) should be (true)
	}
	it should "evaluate to false if stream does not start with arg" in {
		val s1 = Stream(1,2,3,4,5)
		val s2 = Stream(2,3)
		s1.startsWith(s2) should be (false)
	}
	it should "evaluate to false if stream is empty" in {
		val s1 = Stream.empty[Int]
		val s2 = Stream(1,2,3)
		s1.startsWith(s2) should be (false)
	}
	it should "evaluate to false if stream is shorter" in {
		val s1 = Stream(1,2,3,4,5)
		val s2 = Stream(1,2,3,4,5,6)
		s1.startsWith(s2) should be (false)
	}
	it should "evaluate to true if arg is empty (same as List.startsWith)" in {
		val s1 = Stream(1,2,3)
		val s2 = Stream.empty
		s1.startsWith(s2) should be (true)
	}
	behavior of "Stream.tails"
	it should "return a stream of streams" in {
		val s = Stream(1,2,3).tails
		val result = s.toList.map{ss => ss.toList}
		result should be (List(List(1,2,3),List(2,3),List(3),List()))
	}
	it should "return an empty stream if stream is empty" in {
		val s = Stream.empty.tails
		val result = s.toList.map{ss => ss.toList}
		result should be (List(List()))
	}
	behavior of "Stream.tailsUnfold"
	it should "return a stream of streams" in {
		val s = Stream(1,2,3).tailsUnfold
		val result = s.toList.map{ss => ss.toList}
		result should be (List(List(1,2,3),List(2,3),List(3),List()))
	}
	it should "return an empty stream if stream is empty" in {
		val s = Stream.empty.tailsUnfold
		val result = s.toList.map{ss => ss.toList}
		result should be (List(List()))
	}
	behavior of "Stream.scanRight"
	it should "scan Stream from right and apply function" in {
		val s = Stream (1,2,3)
		s.scanRight(0)(_ + _).toList should be (List(6,5,3,0))
	}
	it should "return value if stream is empty" in {
		val s = Stream.empty[Int]
		s.scanRight(0)(_ + _).toList should be (List(0))
	}
}
