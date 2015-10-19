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
	it should "hold for wc monoid" in {
		val stubGen:Gen[WC] = genString.map(Stub)
		val partGen:Gen[WC] = genString.listOfN(2).flatMap(l => genInt.map(i => Part(l.head, i, l.tail.head)))
		implicit val wcGen = Gen.union(stubGen, partGen)
		wcMonoid.check
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

	behavior of "count (number of words in a string)"
	it should "count the number of words in an empty string" in {
		count("") should be (0)
	}
	it should "count the number of words in an single string" in {
		count("foo") should be (1)
	}
	it should "count the number of words in a string" in {
		count("lorem ipsum dolor sit amet") should be (5)
	}

	behavior of "map merge monoid"
	it should "merge maps" in {
		val m1 = Map("a" ->"larry", "b" -> "moe")
		val m2 = Map("c" -> "curly")
		val monoid:Monoid[Map[String,String]] = Monoid.mapMergeMonoid(stringMonoid)
		val result = monoid.op(m1,m2)
		result should be (Map("a" -> "larry", "b" -> "moe", "c" -> "curly"))
	}
	it should "merge maps with duplicates" in {
		val m1 = Map("a" ->"larry", "b" -> "moe")
		val m2 = Map("c" -> "curly","a" -> "ellison")
		val monoid:Monoid[Map[String,String]] = Monoid.mapMergeMonoid(stringMonoid)
		val result = monoid.op(m1,m2)
		result should be (Map("a" -> "larryellison", "b" -> "moe", "c" -> "curly"))
	}
}
