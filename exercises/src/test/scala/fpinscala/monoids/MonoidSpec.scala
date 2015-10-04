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
	behavior of "Monoid laws"
	it should "hold for int addition monoid" in {
		val m = Monoid.intAddition
		val g = Gen.choose(Int.MinValue,Int.MaxValue)
		Monoid.monoidLaws(m)(g)
	}
	it should "hold for int multiplication monoid" in {
		val m = Monoid.intMultiplication
		val g = Gen.choose(Int.MinValue,Int.MaxValue)
		Monoid.monoidLaws(m)(g)
	}
	it should "hold for boolean AND monoid" in {
		val m = Monoid.booleanAnd
		val g = Gen.boolean
		Monoid.monoidLaws(m)(g)
	}
	it should "hold for boolean OR monoid" in {
		val m = Monoid.booleanOr
		val g = Gen.boolean
		Monoid.monoidLaws(m)(g)
	}
	it should "hold for string addition monoid" in {
		val m = Monoid.stringMonoid
		val g = Gen.choose(Int.MinValue,Int.MaxValue).map(_.toString)
		Monoid.monoidLaws(m)(g)
	}

}
