package fpinscala.gettingstarted

import org.scalatest.{Matchers, FlatSpec}
import fpinscala.gettingstarted.MyModule._
import fpinscala.gettingstarted.PolymorphicFunctions._
import org.scalacheck.Prop.forAll
/**
 * Created with IntelliJ IDEA.
 * Date: 15-06-05
 * Time: 11:18 PM
 * To change this template use File | Settings | File Templates.
 */
class GettingStartedSpec extends FlatSpec with Matchers {
	behavior of "Fibonacci function"
	it should "compute the Nth fibonacci number" in {
		val expected = List(0,1,1,2,3,5,8)
		List(fib(0), fib(1), fib(2), fib(3), fib(4), fib(5), fib(6)) should be (expected)
	}

	behavior of "isSorted function"
	it should "check whether an `Array[A]` is sorted" in {
		val as = Seq(1, 2, 3, 4, 5)
		val comparator = (a: Int, b: Int) => a < b
		isSorted(as.toArray, comparator) should be (true)
		isSorted(as.reverse.toArray, comparator) should be (false)
		isSorted(Array.empty, comparator) should be (true)
	}
	it should "check whether a large `Array[A]` is sorted" in {
		val as = Range(1, 10000000)
		val comparator = (a: Int, b: Int) => a < b
		isSorted(as.toArray, comparator) should be (true)
		isSorted(as.reverse.toArray, comparator) should be (false)
	}

	behavior of "curry function"
	it should "return a curried form" in {
		val f = (a: Int, b: Int) => a + b
		val curried = curry(f)
		f(1, 2) should be  (curried(1)(2))
	}
	it should "return a curried form with ScalaCheck" in {
		val f = (a: Int, b: Int) => a + b
		val curried = curry(f)
		forAll { (a:Int, b:Int) =>
			f(a, b) equals curried(a)(b)
		}.check
	}

	behavior of "uncurry function"
	it should "uncurry a curried function" in {
		val f = (a: Int, b: Int) => a + b
		val curried = curry(f)
		val uncurried = uncurry(curried)
		f(1, 2) should be (uncurried(1,2))
	}
	it should "uncurry a curried function with ScalaCheck" in {
		val f = (a: Int, b: Int) => a + b
		val curried = curry(f)
		val uncurried = uncurry(curried)
		forAll { (a:Int, b:Int) =>
			f(a, b) equals uncurried(a, b)
		}.check
	}

	behavior of "compose function"
	it should "compose two functions" in {
		val f = (a:Int) => a + 10
		val g = (b:Int) => b * 10
		val composed = compose(f, g)
		composed(10) should be (f(g(10)))
	}

}
