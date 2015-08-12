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
	behavior of "ParOps infix operators"
	it should "run" in {
		unit(1).run(executor).get should be (1)
	}
	it should "allow map" in {
		unit(1).map(_.toString).run(executor).get() should be ("1")
	}

}
