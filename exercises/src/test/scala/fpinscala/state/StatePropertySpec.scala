package fpinscala.state

import fpinscala.state.RNG.Simple
import org.scalacheck.Gen
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ShouldMatchers, Matchers, PropSpec}
import org.scalacheck.Prop.forAll

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-27
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
class StatePropertySpec extends PropSpec with PropertyChecks with Matchers{
	val smallInteger = Gen.choose(0,100)
	val longSeed =  Gen.choose(Long.MinValue,Long.MaxValue)

	property("State.nonNegative must return a positive integer for all seeds") {
		forAll { (seed: Long) =>
			val (next, nextState) = RNG.nonNegativeInt(Simple(seed))
			next should be >= 0
		}
	}
	property("State.double must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next, _)  = RNG.double(Simple(seed))
			next should (be < 1.0 and be >= 0.0)
		}
	}
	property("State.doubleWithMap must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next,_) = RNG.doubleWithMap(Simple(seed))
			next should (be < 1.0 and be >= 0.0)
		}
	}
	property("State.intDouble must return pairs on int and double in the expected range") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = RNG.intDouble(Simple(seed))
			i should (be > Int.MinValue and be < Int.MaxValue)
			d should (be < 1.0 and be >= 0.0)
		}
	}
	property("State.intDouble must return pairs on int and double that are different from each other") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = RNG.intDouble(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i should (not equal doubleAsInt)
		}
	}
	property("State.doubleInt must return pairs on double and int in the expected range") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = RNG.doubleInt(Simple(seed))
			i should (be > Int.MinValue and be < Int.MaxValue)
			d should (be < 1.0 and be >= 0.0)
		}
	}
	property("State.doubleInt must return pairs on double and int that are different from each other") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = RNG.doubleInt(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i should (not equal doubleAsInt)
		}
	}
	property("State.double3 must return tuple of doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = RNG.double3(Simple(seed))
			a should (be < 1.0 and be >= 0.0)
			b should (be < 1.0 and be >= 0.0)
			c should (be < 1.0 and be >= 0.0)
		}
	}
	property("State.double3 must return tuple of unique doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = RNG.double3(Simple(seed))
			a should (not equal b)
			b should (not equal c)
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
