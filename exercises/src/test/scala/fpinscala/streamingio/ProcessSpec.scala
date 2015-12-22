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
  it should "count" in {
    val s = Stream(1,2,3,4)
    val result = Process.count(s).toList
    result should be (List(1,2,3,4))
  }
  it should "count empty" in {
    val result = Process.count(Stream.empty[Int]).toList
    result should be ('empty)
  }
  it should "compute a running mean" in {
    val s = Stream[Double](1.0,2.0,3.0,4.0)
    val result = Process.mean(s).toList
    result should be (List(1,1.5, 2,2.5))
  }
  it should "count3" in {
    val s = Stream(1,2,3,4)
    val result = Process.count3(s).toList
    result should be (List(1,2,3,4))
  }
  it should "count3 empty" in {
    val result = Process.count3(Stream.empty[Int]).toList
    result should be ('empty)
  }
  it should "sum2" in {
    val result = Process.sum2(Stream(1,2,3,4)).toList
    result should be (List(1,3,6,10))
  }
  it should "sum2 on empty" in {
    val result = Process.sum2(Stream.empty[Double]).toList
    result should be ('empty)
  }
  it should "zipWithIndex" in {
    val result = Process.zipWithIndex(Stream("a","b","c","d")).toList
    result should be (List(("a",0),("b",1),("c",2),("d",3)))
  }
  it should "zip" in {
    val p:Process[Int,(Int,Int)]  = Process.zip(Process.count, Process.count)
    val result = p(Stream(1,2,3,4)).toList
    result should be (List((1,1),(2,2),(3,3),(4,4)))
  }
  it should "exists" in {
    val result = Process.exists[Int](_ % 2 == 0)(Stream(1,3,5,6,7))
    result should be (Stream(false,false,false,true,false))
  }
  it should "toCelsius" in {
    val result = (Process.processToCelsius |> Process.lift(d => d.round))(Stream("32","0","20"))
    result should be (Stream(0,-18,-7))
  }
  it should "toCelsius with empty" in {
    val result = (Process.processToCelsius |> Process.lift(d => d.round))(Stream("", "32","0","20"))
    result should be (Stream(0,-18,-7))
  }
  it should "toCelsius with #" in {
    val result = (Process.processToCelsius |> Process.lift(d => d.round))(Stream("#", "32","0","20"))
    result should be (Stream(0,-18,-7))
  }
  behavior of "|> operator"

  it should "pipe two proceses" in {
    val p:Process[Int,Int] = Process.take(2) |> Process.count
    val result = p(Stream(1,2,3,4,5)).toList
    result should be (List(1,2))
  }
  it should "work on empty" in {
    val p:Process[Int,Int] = Process.take(2) |> Process.count
    val result = p(Stream.empty[Int]).toList
    result should be ('empty)
  }
}
