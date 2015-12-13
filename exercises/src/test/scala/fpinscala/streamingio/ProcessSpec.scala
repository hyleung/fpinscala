package fpinscala.streamingio

import org.scalatest.{FlatSpec, Matchers}
import fpinscala.streamingio.SimpleStreamTransducers._

class ProcessSpec extends FlatSpec with Matchers {

  lazy val fibs:Stream[Int] = {
    def loop(h:Int,n:Int):Stream[Int] = h #:: loop(n, h + n)
    loop(1,1)
  }
  behavior of "Process"
  it should "take(n)" in {
    val p = Process.take[Int](5)
    val result = p(fibs).toList
    result.size should be (5)
  }
}
