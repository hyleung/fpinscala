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
  behavior of "list.setHead"
  it should "error when setting the head for an empty list"  in {
    val list = List()
    an [Exception] should be thrownBy  List.setHead(list, 1)
  }
  it should "set the head for a list" in {
    val list = List(1,2,3,4)
    List.setHead(list, 10) should be (List(10,2,3,4))
  }
  behavior of "list.drop(n)"
  it should "drop the first `n` elements" in {
    val list  = List(1,2,3,4,5,6,7,8,9,10)
    List.drop(list,5) should be (List(6,7,8,9,10))
  }
  it should "return empty list when dropping from empty list" in {
    val list  = List()
    List.drop(list,5) should be (List())
  }
  it should "return empty list when dropping from list that is smaller than `n`" in {
    val list  = List(1,2,3,4)
    List.drop(list,5) should be (List())
  }
  it should "return list if n < 0" in {
    val list  = List(1,2,3,4)
    List.drop(list,-1) should be (list)
  }
  behavior of "list.dropWhile(f)"
  it should "drop all elements matching predicate" in {
    val list = List(1,2,3,4,5,6,7,8,9,10)
    List.dropWhile(list,(x:Int) => x <= 8) should be (List(9,10))
  }
  it should "return empty list when applied to empty list" in {
    val list = List()
    List.dropWhile(list, (x:Int) => x <= 8) should be (List())
  }
}
