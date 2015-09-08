package fpinscala.testing

import java.util.concurrent.{Executors, ExecutorService}

import fpinscala.parallelism.Par
import fpinscala.parallelism.Par
import fpinscala.parallelism.Par.Par
import fpinscala.state.RNG.Simple
import fpinscala.testing.Prop.{Proved, Falsified, Passed, Result}
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-30
 * Time: 10:54 AM
 * To change this template use File | Settings | File Templates.
 */
class PropSpec extends FlatSpec with Matchers {
	behavior of "Prop.forAll"
	it should "check property for all passing" in {
		val p = Prop.forAll(Gen.unit(1).unsized)(_ == 1)
		p.run(100, 100, Simple(1l)) should be(Passed)
	}
	it should "check property for one failing" in {
		val p = Prop.forAll(Gen.choose(1, 100).unsized)(_ != 50)
		p.run(100, 1000, Simple(1l)).isFalsified should be(true)
	}
	it should "check failing property for with correct count" in {
		val p = Prop.forAll(Gen.choose(1, 100).unsized)(_ <= 50)
		p.run(100, 1000, Simple(1l)) match {
			case Falsified(failedCase, count) => count should be > 0
			case _ => fail()
		}

	}
	it should "fail if exception is thrown in predicate" in {
		val p = Prop.forAll(Gen.unit(1).unsized)(i => throw new RuntimeException)
		val result: Result = p.run(100, 1000, Simple(1l))
		result.isFalsified should be(true)
	}
	behavior of "Prop.&&"
	it should "evaluate to Passed" in {
		val g = Gen.unit(50).unsized
		val p1 = Prop.forAll(g)(_ == 50)
		val p2 = Prop.forAll(g)(_ * 2 == 100)
		val p = p1.&&(p2)
		val result = p.run(100, 10, Simple(1l))
		result.isFalsified should be (false)
	}
	it should "evaluate to Falsified" in {
		val g = Gen.unit(50).unsized
		val p1 = Prop.forAll(g)(_ == 50)
		val p2 = Prop.forAll(g)(_ != 50)
		val p = p1.&&(p2)
		val result = p.run(100, 10, Simple(1l))
		result.isFalsified should be (true)
	}
	behavior of "Prop.||"
	it should "evaluate Passed with first param" in {
		val g = Gen.unit(50).unsized
		val p1 = Prop.forAll(g)(_ == 50)
		val p2 = Prop.forAll(g)(_ != 50)
		val p = p1.||(p2)
		val result = p.run(100, 10, Simple(1l))
		result.isFalsified should be (false)
	}
	it should "evaluate Passed with second param" in {
		val g = Gen.unit(50).unsized
		val p1 = Prop.forAll(g)(_ != 50)
		val p2 = Prop.forAll(g)(_ == 50)
		val p = p1.||(p2)
		val result = p.run(100, 10, Simple(1l))
		result.isFalsified should be (false)
	}
	it should "evaluate Falsified" in {
		val g = Gen.unit(50).unsized
		val p1 = Prop.forAll(g)(_ != 50) //Falsified
		val p2 = Prop.forAll(g)(_ * 2 != 100) //Falsified
		val p = p1.||(p2)
		val result = p.run(100, 10, Simple(1l))
		result.isFalsified should be (true)
	}
	behavior of "check"
	it should "evaluate to Proved for boolean" in {
		val r = Prop.check(true).run(10,10, Simple(1l))
		r should be (Passed)
	}
	it should "enable testing of Par" in {
		val ES = Executors.newCachedThreadPool()
		val p2 = Prop.check{
			val p = Par.map(Par.unit(1))(_ + 1)
			val p2 = Par.unit(2)
			p(ES).get() == p2(ES).get()
		}
		Prop.run(p2)
	}
	it should "enable testing of Par with lifted equals" in {
		def equal[A](p:Par[A], p2:Par[A]):Par[Boolean] =
			Par.map2(p,p2)(_ == _)
		val ES = Executors.newCachedThreadPool()
		val parEqual = equal(
			Par.map(Par.unit(1))(_ + 1),
			Par.unit(2)
		)
		val p3 = Prop.check(parEqual(ES).get())
		Prop.run(p3)
	}
	it should "verify 'map(y)(x => x) == y'" in {
		def equal[A](p:Par[A], p2:Par[A]):Par[Boolean] =
			Par.map2(p,p2)(_ == _)
		val pint = Gen.choose(0,10).map(Par.unit(_)) //Par of Int
		val p = Prop.forAllPar(pint)(n => equal(Par.map(n)(y => y), n))
		Prop.run(p)
	}
}
