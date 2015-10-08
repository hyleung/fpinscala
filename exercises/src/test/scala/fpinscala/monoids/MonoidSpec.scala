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
	import Monoid.MonoidLaws.toCheckable
	import Monoid._
	implicit val genInt = Gen.choose(Int.MinValue,Int.MaxValue)
	implicit val genBool = Gen.boolean
	implicit val genString = genInt.map(_.toString)
	behavior of "Monoid laws"
	it should "hold for int addition monoid" in {
		intAddition.check
	}
	it should "hold for int multiplication monoid" in {
		intMultiplication.check
	}
	it should "hold for boolean AND monoid" in {
		booleanAnd.check
	}
	it should "hold for boolean OR monoid" in {
		booleanOr.check
	}
	it should "hold for string addition monoid" in {
		stringMonoid.check
	}

	behavior of "concatenate"
	it should "work for ints" in {
		val list = List(1,2,3,4,5)
		val result = Monoid.concatenate(list,Monoid.intAddition)
		result should be (15)
	}
	it should "work for strings" in {
		val list = List("shazam","and","abra","cadabra")
		val result = Monoid.concatenate(list, Monoid.stringMonoid)
		result should be ("shazamandabracadabra")
	}

	behavior of "foldMap"
	it should "map function over list with intAddition" in {
		val list = List(1,2,3,4,5)
		val result = Monoid.foldMap(list,Monoid.intAddition)(x => x * 2)
		result should be (30)
	}
	it should "map function over list with string monoid" in {
		val list = List(1,2,3,4,5)
		val result = Monoid.foldMap(list,Monoid.stringMonoid)(x => x.toString)
		result should be ("12345")
	}

	behavior of "foldMapV"
	it should "map function over list with intAddiion" in {
		val list = Range(1,6)
		val result = Monoid.foldMapV(list,Monoid.intAddition)(x => x * 2)
		result should be (30)
	}
}
