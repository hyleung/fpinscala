package fpinscala.datastructures

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by hyleung on 15-06-08.
 */
class ListSpec extends FlatSpec with Matchers{
  behavior of "x"
  it should "evaluate to expected value (matches on 3rd Case)" in  {
    List.x should be (3)
  }
  behavior of "List.tail"
  it should "return tail of list" in {
    val list = List(1,2,3,4)
    List.tail(list) should be (List(2,3,4))
  }
  it should "return empty list for tail of singleton list" in {
    val list = List(1)
    List.tail(list) should be (List())
  }
  it should "return empty list for tail of empty list" in {
    val list = List()
    List.tail(list) should be (List())
  }
}
