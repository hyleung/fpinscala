package fpinscala.testing

import fpinscala.state.RNG.Simple
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-22
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
class PropSpec extends FlatSpec with Matchers{
	behavior of "Gen.choose"
	it should "produce a number between 1 and 10" in {
		val choose = Gen.choose(1,10)
		val (v,_) =  choose.sample.run(Simple(1l))
		v should (be <=10 and be >= 1)
	}
	behavior of "Gen.unit"
	it should "produce the provided value" in {
		val unit = Gen.unit(1)
		val (v,_) = unit.sample.run(Simple(1l))
		v should be (1)
	}
	behavior of "Gen.boolean"
	it should "produce a boolean" in {
		val b = Gen.boolean
		val (v,_) = b.sample.run(Simple(1l))
		//uh....really?...
		v should (be (true) or be (false))
	}
	behavior of "Gen.listOfN"
	it should "produce list of N size" in {
		val l = Gen.listOfN(10,Gen.unit(1))
		val (v,_) = l.sample.run(Simple(1l))
		v should have length 10
		v should be (List.fill(10)(1))
	}

}
