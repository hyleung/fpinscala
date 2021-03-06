package fpinscala.parallelism

import java.util.concurrent.{TimeUnit, ForkJoinPool, ThreadPoolExecutor, ExecutorService}
import Par._
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-09
 * Time: 2:19 PM
 * To change this template use File | Settings | File Templates.
 */
class ParSpec extends FlatSpec with Matchers{
	val executor = new ForkJoinPool()
	behavior of "Non-parallel sum"
	it should "sum an empty list of int" in {
		val list = IndexedSeq.empty[Int]
		Examples.sum(list) should be (0)
	}
	it should "sum a singleton list of int" in {
		val list = IndexedSeq(1)
		Examples.sum(list) should be (1)
	}
	it should "sum a list of int" in {
		val list = IndexedSeq.fill(100)(1)
		Examples.sum(list) should be (100)
	}
	behavior of "Parallel sum"
	it should "sum an empty list of int" in {
		val list = IndexedSeq.empty[Int]
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS) should be (0)
	}
	it should "sum a singleton list of int" in {
		val list = IndexedSeq(1)
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS) should be (1)
	}
	it should "sum a list of int" in {
		val list = IndexedSeq.fill(100)(1)
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS)  should be (100)
	}
	behavior of "asyncF"
	it should "convert any A => B to a Par[B]" in {
		val f = (a:Int) => s"Hello $a"
		val async = Par.asyncF(f)
		Par.run(executor)(async(10)).get() should be ("Hello 10")
	}
	behavior of "lazyUnit"
	it should "create a lazy evaluated unit" in {
		val u = Par.lazyUnit(1)
		Par.run(executor)(u).get() should be (1)
	}

	behavior of "ParOps infix operators"
	it should "run" in {
		unit(1).run(executor).get should be (1)
	}
	it should "fork" in {
		unit(1).fork.run(executor).get should be (1)
	}
	it should "allow map" in {
		unit(1).map(_.toString).run(executor).get() should be ("1")
	}
	it should "allow map2" in {
		unit(1)
			.map2(unit(2))((a,b) => a + b)
			.run(executor).get() should be (3)
	}
	it should "allow equal" in {
		unit(1).equal(unit(1))(executor) should be (true)
	}

	behavior of "sequence"
	it should "sequence a list of Par[A]" in {
		val l = List(unit(1),unit(2),unit(3))
		val p = Par.sequence(l)
		p.run(executor).get() should be (List(1,2,3))
	}

	behavior of "parMap"
	it should "parallelize application of function to sequence of A" in {
		val l = List(1,2,3)
		val p = Par.parMap(l)(_ * 2)
		p.run(executor).get() should be (List(2,4,6))
	}

	behavior of "parFilter"
	it should "apply filter predicate to a sequence of A in parallel" in {
		val l = Range(1,11).toList
		val p = Par.parFilter(l)(_ % 2 == 0)
		p.run(executor).get() should be (List(2,4,6,8,10))
	}

	behavior of "Par.map3"
	it should "map function over 3 Par" in {
		val p1 = unit(1)
		val p2 = unit(2)
		val p3 = unit(3)
		val p = Par.map3(p1, p2, p3)((a,b,c) => a + b +c)
		p.run(executor).get() should be (6)
	}

	behavior of "Par.choice"
	it should "return first param if predicate evaluates to true" in {
		val a = unit(1)
		val b = unit(2)
		val result = Par.choice(unit(true))(a,b)
		result.run(executor).get()  should be (1)
	}
	it should "return second param if predicate evaluates to false" in {
		val a = unit(1)
		val b = unit(2)
		val result = Par.choice(unit(false))(a,b)
		result.run(executor).get()  should be (2)
	}

	behavior of "Par.choiceN"
	it should "return the Nth value" in {
		val l = List(unit(1),unit(2),unit(3),unit(4))
		val result = Par.choiceN(unit(2))(l)
		result.run(executor).get() should be(3)
	}

	behavior of "Par._choice"
	it should "return first param if predicate evaluates to true" in {
		val a = unit(1)
		val b = unit(2)
		val result = Par._choice(unit(true))(a,b)
		result.run(executor).get()  should be (1)
	}
	it should "return second param if predicate evaluates to false" in {
		val a = unit(1)
		val b = unit(2)
		val result = Par._choice(unit(false))(a,b)
		result.run(executor).get()  should be (2)
	}

	behavior of "Par.choiceMap"
	it should "return the value from the Par[A] associated with the key" in {
		val choices = Map("one" -> unit(1), "two" -> unit(2), "three" -> unit(3))
		val pk =  unit("two")
		val result = Par.choiceMap(pk)(choices)
		result.run(executor).get() should be (2)
	}
	behavior of "Par.flapMap"
	it should "return allow implementation of choiceN" in {
		val l = List(unit(1),unit(2),unit(3),unit(4))
		val pa = unit(1)
		val result = Par.flatMap(pa)(l)
		result.run(executor).get() should be (2)
	}
	it should "return allow implementation of choice" in {
		val l = asyncF((a:Boolean) => if (a) "yay!" else "boo!" )
		val pa = unit(true)
		val result = Par.flatMap(pa)(l)
		result.run(executor).get() should be ("yay!")
	}
	it should "return allow implementation of choiceN with infix" in {
		val l = List(unit(1),unit(2),unit(3),unit(4))
		val pa = unit(1)
		val result = pa.flatMap(l)
		result.run(executor).get() should be (2)
	}
	it should "return allow implementation of choice with infix" in {
		val l = asyncF((a:Boolean) => if (a) "yay!" else "boo!" )
		val pa = unit(true)
		val result = pa.flatMap(l)
		result.run(executor).get() should be ("yay!")
	}
	behavior of "Par.join"
	it should "join two Par computations" in {
		val ppa = unit(unit(1))
		Par.join(ppa)(executor).get() should be (1)
	}
}
