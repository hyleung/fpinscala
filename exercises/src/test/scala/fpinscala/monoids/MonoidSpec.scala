package fpinscala.monoids

import fpinscala.testing.Gen
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-10-02
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
class MonoidSpec extends FlatSpec with Matchers{
	behavior of "int addition monoid"
	it should "obey monoid laws" in {
		val m = Monoid.intAddition
		val g = Gen.choose(Int.MinValue,Int.MaxValue)
		Monoid.monoidLaws[Int](m,g)
	}
	behavior of "int multiplication monoid"
	it should "obey monoid laws" in {
		val m = Monoid.intMultiplication
		val g = Gen.choose(Int.MinValue,Int.MaxValue)
		Monoid.monoidLaws[Int](m,g)
	}

}
