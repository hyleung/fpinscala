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
class StatePropertySpec extends PropSpec with PropertyChecks {
	property("State.nonNegative must return a positive integer for all seeds") {
		forAll { (seed: Long) =>
			val (next, nextState) = RNG.nonNegativeInt(Simple(seed))
			next >= 0 should be(true)
		}
	}
	property("State.double must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next, nextState) = RNG.double(Simple(seed))
			(next < 1 & next >= 0) should be(true)
		}
	}
	property("State.intDouble must return pairs on int and double in the expected range") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = RNG.intDouble(Simple(seed))
			(i > Int.MinValue & i < Int.MaxValue) should be(true)
			(d < 1 & d >= 0) should be(true)
		}
	}
	property("State.intDouble must return pairs on int and double that are different from each other") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = RNG.intDouble(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i == doubleAsInt should be(false)
		}
	}
	property("State.doubleInt must return pairs on double and int in the expected range") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = RNG.doubleInt(Simple(seed))
			(i > Int.MinValue & i < Int.MaxValue) should be(true)
			(d < 1 & d >= 0) should be(true)
		}
	}
	property("State.doubleInt must return pairs on double and int that are different from each other") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = RNG.doubleInt(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i == doubleAsInt should be(false)
		}
	}
}
