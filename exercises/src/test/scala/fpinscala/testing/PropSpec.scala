package fpinscala.testing

import fpinscala.state.RNG.Simple
import fpinscala.testing.Prop.{Passed, Result}
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
		val p = Prop.forAll(Gen.unit(1))(_ == 1)
		p.run(100, Simple(1l)) should be(Passed)
	}
	it should "check property for one failing" in {
		val p = Prop.forAll(Gen.choose(1, 100))(_ != 50)
		p.run(1000, Simple(1l)).isFalsified should be(true)
	}
	it should "fail if exception is thrown in predicate" in {
		val p = Prop.forAll(Gen.unit(1))(i => throw new RuntimeException)
		val result: Result = p.run(1000, Simple(1l))
		result.isFalsified should be(true)
	}
}
