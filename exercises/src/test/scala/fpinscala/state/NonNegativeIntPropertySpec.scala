package fpinscala.state

import fpinscala.state.RNG.Simple
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-27
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
class NonNegativeIntPropertySpec extends PropSpec with PropertyChecks {
	property("State.nonNegative must return a positive integer for all seeds") {
		forAll { (seed: Long) =>
			val (next, nextState) = RNG.nonNegativeInt(Simple(seed))
			next >= 0 shouldBe (true)
		}

	}

}
