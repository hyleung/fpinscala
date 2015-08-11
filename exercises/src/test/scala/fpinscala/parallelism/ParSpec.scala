package fpinscala.parallelism

import java.util.concurrent.{TimeUnit, ForkJoinPool, ThreadPoolExecutor, ExecutorService}

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-09
 * Time: 2:19 PM
 * To change this template use File | Settings | File Templates.
 */
class ParSpec extends FlatSpec with Matchers{
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
		val executor = new ForkJoinPool()
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS) should be (0)
	}
	it should "sum a singleton list of int" in {
		val list = IndexedSeq(1)
		val executor = new ForkJoinPool()
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS) should be (1)
	}
	it should "sum a list of int" in {
		val list = IndexedSeq.fill(100)(1)
		val executor = new ForkJoinPool()
		val parSum = Examples.sumPar(list)
		Par.run(executor)(parSum).get(50, TimeUnit.MILLISECONDS)  should be (100)
	}
}
