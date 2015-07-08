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
}