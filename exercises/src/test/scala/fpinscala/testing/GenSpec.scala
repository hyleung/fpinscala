package fpinscala.testing

import fpinscala.state.{State, RNG}
import fpinscala.state.RNG.Simple
import org.scalatest.{Matchers, FlatSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-22
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
class GenSpec extends FlatSpec with Matchers{
	behavior of "Gen.choose"
	it should "produce a number between 1 and 10" in {
		val choose = Gen.choose(1,10)
		val (v,_) =  choose.sample.run(Simple(1l))
		v should (be <=10 and be >= 1)
	}
	behavior of "Gen.unit"
	it should "produce the provided value" in {
		val unit = Gen.unit(1)
		val (v,_) = unit.sample.run(Simple(1l))
		v should be (1)
	}
	behavior of "Gen.boolean"
	it should "produce a boolean" in {
		val b = Gen.boolean
		val (v,_) = b.sample.run(Simple(1l))
		//uh....really?...
		v should (be (true) or be (false))
	}
	behavior of "Gen.listOfN"
	it should "produce list of N size" in {
		val l = Gen.unit(1).listOfN(10)
		val (v,_) = l.sample.run(Simple(1l))
		v should have length 10
		v should be (List.fill(10)(1))
	}
	behavior of "Gen.flatMap"
	it should "flatMap" in {
		val a = Gen.unit(1)
		val b = a.flatMap(n => Gen.unit(s"We're number $n!"))
		val (v,_) = b.sample.run(Simple(1l))
		v should be ("We're number 1!")
	}
	behavior of "Gen.sameParity"
	it should "generate pairs in range" in {
		val ((a,b),_) = Gen.sameParity(1,10).sample.run(Simple(1l))
		a should (be >=1 and be <= 10)
		b should (be >=1 and be <= 10)
	}
	behavior of "Gen union"
	it should "generate a value" in {
		val g1 = Gen.unit(1)
		val g2 = Gen.unit(2)
		val r = Gen.union(g1, g2)
		val (v,_) = r.sample.run(Simple(1l))
		v should (be (1) or be (2))
	}
	behavior of "Gen.weighted"
	it should "return g1 Gen[A] with weight 1" in {
		val g1 = Gen.unit(1)
		val g2 = Gen.unit(2)
		val r = Gen.weighted((g1,1),(g2,0))
		val (v,_) = r.sample.run(Simple(1l))
		v should be (1)
 	}
	it should "return g2 Gen[A] with weight 1" in {
		val g1 = Gen.unit(1)
		val g2 = Gen.unit(2)
		val r = Gen.weighted((g1,0),(g2,1))
		val (v,_) = r.sample.run(Simple(1l))
		v should be (2)
	}
	it should "return Gen[A] with even weighting" in {
		val g1 = Gen.unit(1)
		val g2 = Gen.unit(2)
		val r = Gen.weighted((g1,0.5),(g2,0.5))
		val (v,_) = r.sample.run(Simple(1l))
		v should (be (2) or be (1))
	}
	behavior of "Gen.unsized"
	it should "convert a Gen to an SGen" in {
		val g = Gen.unit(1)
		val s = g.unsized
		val (r,_) = s.forSize(1).sample.run(Simple(1l))
		r should be (1)
	}
	behavior of "Gen.listOf"
	it should "return an SGen of List of A" in {
		val g = Gen.unit(1)
		val s = Gen.listOf(g)
		val (r,_) = s.forSize(3).sample.run(Simple(1l))
		r should be (List(1,1,1))
	}
	behavior of "max"
	"The maximum of a list" should "be greater than or equal to every other element" in {
		val smallInt = Gen.choose(-10,10)
		val maxProp = Prop.forAll(Gen.listOf1(smallInt)) { ns =>
			val max = ns.max
			!ns.exists(_ > max) //there exists no element greater than the max
		}
		Prop.run(maxProp)
	}
	behavior of "List.sorted"
	it should "return the sorted list" in {
		val ints = Gen(State(RNG.nonNegativeInt))
		val s = Gen.listOf(ints)
		val sortedProp = Prop.forAll(s){l =>
			val sorted = l.sorted
			sorted.isEmpty ||
			sorted.tail.isEmpty ||
			!sorted.zip(sorted.tail).exists{ case (a,b) => a > b }
		}
		Prop.run(sortedProp)
	}
}
