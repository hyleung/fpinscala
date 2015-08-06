package fpinscala.state

import fpinscala.state.RNG._
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-05
 * Time: 7:32 PM
 * To change this template use File | Settings | File Templates.
 */
class StateSpec extends FlatSpec with Matchers{
	behavior of "State.flatMap"
	it should "apply function to value and return next state" in {
		val s = State(int)
		val (result,_) = s.flatMap(a => State.unit(a)).run(Simple(100))
		result should (be < Int.MaxValue and be > Int.MinValue)
	}
	behavior of "State.map"
	it should "apply function to value and return next state" in {
		val s = State(int)
		val (result,_) = s.map(_ * 2).run(Simple(100))
		result should (be < Int.MaxValue and be > Int.MinValue)
	}
}
