package fpinscala.testing

import fpinscala.state.RNG.Simple
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-02
 * Time: 6:45 PM
 * To change this template use File | Settings | File Templates.
 */
class SGenSpec extends FlatSpec with Matchers{
	behavior of "SGen.forAll"
	it should "verify property for single" in {
		val s = Gen.unit(1).unsized
		val r = SGen.forAll(s)(_ == 1).run(100, 1000,Simple(1l))
		r.isFalsified should be (false)
	}
	it should "verify property with list" in {
		val s = Gen.choose(1,100).unsized
		val r = SGen.forAll(s)(_ <= 50).run(100, 1000,Simple(1l))
		r.isFalsified should be (true)
	}
}
