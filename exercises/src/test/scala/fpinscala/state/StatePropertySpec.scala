package fpinscala.state

import fpinscala.state.RNG._
import org.scalacheck.Gen

import org.scalatest.prop.PropertyChecks
import org.scalatest.{ShouldMatchers, Matchers, PropSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-27
 * Time: 6:00 PM
 * To change this template use File | Settings | File Templates.
 */
class StatePropertySpec extends PropSpec with PropertyChecks with Matchers{
	val smallInteger = Gen.choose(1,100)
	val longSeed =  Gen.choose(Long.MinValue,Long.MaxValue)

	property("nonNegative must return a positive integer for all seeds") {
		forAll { (seed: Long) =>
			val (next, nextState) = nonNegativeInt(Simple(seed))
			next should be >= 0
		}
	}
	property("double must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next, _)  = double(Simple(seed))
			next should (be < 1.0 and be >= 0.0)
		}
	}
	property("doubleWithMap must return double between 0 and 1, not including 1") {
		forAll { (seed: Long) =>
			val (next,_) = doubleWithMap(Simple(seed))
			next should (be < 1.0 and be >= 0.0)
		}
	}
	property("intDouble must return pairs on int and double in the expected range") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = intDouble(Simple(seed))
			i should (be > Int.MinValue and be < Int.MaxValue)
			d should (be < 1.0 and be >= 0.0)
		}
	}
	property("intDouble must return pairs on int and double that are different from each other") {
		forAll { (seed: Long) =>
			val ((i, d), nextState) = intDouble(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i should (not equal doubleAsInt)
		}
	}
	property("doubleInt must return pairs on double and int in the expected range") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = doubleInt(Simple(seed))
			i should (be > Int.MinValue and be < Int.MaxValue)
			d should (be < 1.0 and be >= 0.0)
		}
	}
	property("doubleInt must return pairs on double and int that are different from each other") {
		forAll { (seed: Long) =>
			val ((d, i), nextState) = doubleInt(Simple(seed))
			val doubleAsInt = (d * Double.MaxValue).toInt
			i should (not equal doubleAsInt)
		}
	}
	property("double3 must return tuple of doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = double3(Simple(seed))
			a should (be < 1.0 and be >= 0.0)
			b should (be < 1.0 and be >= 0.0)
			c should (be < 1.0 and be >= 0.0)
		}
	}
	property("double3 must return tuple of unique doubles") {
		forAll { (seed:Long) =>
			val ((a,b,c), next) = double3(Simple(seed))
			a should (not equal b)
			b should (not equal c)
		}
	}
	property("ints must return list of ints") {
		forAll(longSeed, smallInteger) { (seed:Long, count:Int) =>
			val (result, _) = ints(count)(Simple(seed))
			result.forall{ i => i > Int.MinValue & i < Int.MaxValue} should be (true)
		}
	}
	property("ints must return list of ints with correct length") {
		forAll(longSeed, smallInteger) { (seed:Long, count:Int) =>
			val (result, _) = ints(count)(Simple(seed))
			result.size should be (count)
		}
	}

	property("map should apply function") {
		forAll(longSeed){ (seed:Long) =>
			val simple: Simple = Simple(seed)
			val (result, _) = map(int)(a => a * a)(simple)
			result should (be > Int.MinValue and be < Int.MaxValue)
		}
	}

	property("_map should apply function") {
		forAll(longSeed){ (seed:Long) =>
			val simple: Simple = Simple(seed)
			val (result, _) = _map(int)(a => a * a)(simple)
			result should (be > Int.MinValue and be < Int.MaxValue)
		}
	}

	property("map2 should apply function to 2 Rand[A]") {
		forAll(longSeed){ (seed:Long) =>
			val simple: Simple = Simple(seed)
			val (result, _) = map2(int,int)( _ + _)(simple)
			result should (be > Int.MinValue and be < Int.MaxValue)
		}
	}
	property("_map2 should apply function to 2 Rand[A]") {
		forAll(longSeed){ (seed:Long) =>
			val simple: Simple = Simple(seed)
			val (result, _) = _map2(int,int)( _ + _)(simple)
			result should (be > Int.MinValue and be < Int.MaxValue)
		}
	}
	property("sequence should evaluate list of Rand[A]") {
		forAll(longSeed){(seed:Long) =>
			val initialState = Simple(seed)
			val list = List(int, int, int, int)
			val (result, _) = sequence(list)(initialState)
			result should have length 4
		}
	}
	property("positiveLessThan should return Ints less than the argument") {
		def positiveLessThan(n:Int):Rand[Int] = {
			flatMap(nonNegativeInt){ i =>
				val mod = i % n
				if (i + (n - 1) - mod > 0) unit(mod) else positiveLessThan(n)
			}
		}
		forAll(longSeed, smallInteger){(seed:Long, n:Int) =>
			val initialState = Simple(seed)
			val (result, _ ) = positiveLessThan(n)(initialState)
			result should be < n
		}
	}
}
