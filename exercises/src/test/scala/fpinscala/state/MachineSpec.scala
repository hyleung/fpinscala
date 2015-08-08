package fpinscala.state

import org.scalatest.{FlatSpec, GivenWhenThen, FeatureSpec, Matchers}
import State.simulateMachine
/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-06
 * Time: 5:53 PM
 * To change this template use File | Settings | File Templates.
 */
class MachineSpec extends FlatSpec with Matchers {
	behavior of "Machine"
	"Inserting a coin into a locked machine" should "cause it to unlock if there is any candy left" in {
		val m = Machine(locked = true, 1, 10)
		val ((_, _), nextState) = simulateMachine(List(Coin)).run(m)
		nextState.locked should be (false)
	}
	"Inserting a coin into a locked machine" should "not dispense any candy" in {
		val expected = 1
		val m = Machine(locked = true, expected, 10)
		val ((_, _), nextState) = simulateMachine(List(Coin)).run(m)
		nextState.candies should be (expected)
	}
	"Inserting a coin into a locked machine" should "increment the number of coins" in {
		val expected  = 10
		val m = Machine(locked = true, 1, expected)
		val ((_, _), nextState) = simulateMachine(List(Coin)).run(m)
		nextState.coins should be (expected + 1)
	}
	"Turning the knob on an unlocked machine" should "cause it to dispense candy and become locked." in {
		val initialCount = 1
		val m = Machine(locked = false, initialCount, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Turn)).run(m)
		nextState.locked should be (true)
		candies should be (initialCount - 1)
	}
	"Turning the knob on a locked machine" should "do nothing." in {
		val m = Machine(locked = true, 1, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Turn)).run(m)
		nextState.locked should be (true)
		nextState.candies should be (1)
		nextState.coins should be (10)
	}
	"Inserting a coin into an unlocked machine" should "do not change locked state" in {
		val m = Machine(locked = false, 1, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Coin)).run(m)
		nextState.locked should be (false)
	}
	"Inserting a coin into an unlocked machine" should "do not dispense candy" in {
		val m = Machine(locked = false, 1, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Coin)).run(m)
		nextState.candies should be (1)
	}
	"Inserting a coin into an unlocked machine" should "increment number of coins" in {
		val expected = 10
		val m = Machine(locked = false, 1, expected)
		val ((candies, _), nextState) = State.simulateMachine(List(Coin)).run(m)
		nextState.coins should be (expected + 1)
	}
	"A machine that is out of candy" should  "ignore Coin inputs." in {
		val m = Machine(locked = true, 0, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Coin)).run(m)
		nextState.locked should be (true)
	}
	"A machine that is out of candy" should  "ignore Turn inputs." in {
		val m = Machine(locked = false, 0, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Turn)).run(m)
		nextState.candies should be (0)
		nextState.coins should be (10)
	}
	"An unlocked machine that is out of candy" should  "ignore Coin inputs." in {
		val m = Machine(locked = false, 0, 10)
		val ((candies, _), nextState) = State.simulateMachine(List(Coin)).run(m)
		nextState.candies should be (0)
		nextState.coins should be (10)
	}


}
