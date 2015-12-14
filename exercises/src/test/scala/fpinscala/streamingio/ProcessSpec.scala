package fpinscala.streamingio

import org.scalatest.{FlatSpec, Matchers}
import fpinscala.streamingio.SimpleStreamTransducers._

class ProcessSpec extends FlatSpec with Matchers {

  lazy val fibs:Stream[Int] = {
    def loop(h:Int,n:Int):Stream[Int] = h #:: loop(n, h + n)
    loop(1,1)
  }
  lazy val ints:Stream[Int] = {
    def loop(n:Int):Stream[Int] = n #:: loop(n + 1)
    loop(0)
  }
  behavior of "Process"
  it should "take(n)" in {
    val p = Process.take[Int](5)
    val result = p(fibs).toList
    result.size should be (5)
  }
  it should "take(n) of empty" in {
    val p = Process.take[Int](5)
    val result = p(Stream.empty[Int]).toList
    result should be ('empty)
  }
  it should "drop(n)" in {
    val p = Process.drop[Int](5)
    val result = Process.take[Int](5)(p(ints)).toList
    result.head should be (5)
  } 
  it should "drop(n) on empty" in {
    val p = Process.drop[Int](5)
    val result = Process.take[Int](5)(p(Stream.empty[Int])).toList
    result should be ('empty)
  } 
  it should "takeWhile" in {
    val p = Process.takeWhile[Int](i => i < 5)
    val result = p(ints).toList
    result.size should be (5)
  }
  it should "takeWhile on empty" in {
    val p = Process.takeWhile[Int](i => i < 5)
    val result = p(Stream.empty[Int]).toList
    result should be ('empty)
  }
  it should "dropWhile" in {
    val p = Process.dropWhile[Int](i => i < 5)
    val result = Process.take[Int](1)(p(ints)).toList
    result.head should be (5)
  }
  it should "dropWhile on empty" in {
    val p = Process.dropWhile[Int](i => i < 5)
    val result = Process.take[Int](1)(p(Stream.empty[Int])).toList
    result should be ('empty)
  }
}
