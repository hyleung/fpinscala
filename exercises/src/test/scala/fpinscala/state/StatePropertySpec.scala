package fpinscala.state

import fpinscala.state.RNG.Simple
import org.scalacheck.Gen
import org.scalatest.Matchers._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import org.scalacheck.Prop.forAll

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-27
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
class StatePropertySpec extends PropSpec with PropertyChecks {
	val smallInteger = Gen.choose(0,100)
	val longSeed =  Gen.choose(Long.MinValue,Long.MaxValue)

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
	property("State.doubleWithMap must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next,_) = RNG.doubleWithMap(Simple(seed))
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
	property("State.double3 must return tuple of doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = RNG.double3(Simple(seed))
			(a < 1 & a >= 0) should be(true)
			(b < 1 & b >= 0) should be(true)
			(c < 1 & c >= 0) should be(true)
		}
	}
	property("State.double3 must return tuple of unique doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = RNG.double3(Simple(seed))
			a != b should be (true)
			b != c should be (true)
		}
	}
	property("State.ints must return list of ints") {
		forAll(longSeed, smallInteger) { (seed:Long, count:Int) =>
			val (result, _) = RNG.ints(count)(Simple(seed))
			result.forall{ i => i > Int.MinValue & i < Int.MaxValue} should be (true)
		}
	}
	property("State.ints must return list of ints with correct length") {
		forAll(longSeed, smallInteger) { (seed:Long, count:Int) =>
			val (result, _) = RNG.ints(count)(Simple(seed))
			result.size should be (count)
		}
	}
}
