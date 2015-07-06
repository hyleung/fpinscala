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

}
