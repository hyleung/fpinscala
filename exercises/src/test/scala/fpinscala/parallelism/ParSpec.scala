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
}