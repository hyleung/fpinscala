package fpinscala.datastructures

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by hyleung on 15-06-08.
 */
class ListSpec extends FlatSpec with Matchers {
	behavior of "x"
	it should "evaluate to expected value (matches on 3rd Case)" in {
		List.x should be(3)
	}
	behavior of "List.tail"
	it should "return tail of list" in {
		val list = List(1, 2, 3, 4)
		List.tail(list) should be(List(2, 3, 4))
	}
	it should "return empty list for tail of singleton list" in {
		val list = List(1)
		List.tail(list) should be(List())
	}
	it should "return empty list for tail of empty list" in {
		val list = List()
		List.tail(list) should be(List())
	}
	behavior of "list.setHead"
	it should "error when setting the head for an empty list" in {
		val list = List()
		an[Exception] should be thrownBy List.setHead(list, 1)
	}
	it should "set the head for a list" in {
		val list = List(1, 2, 3, 4)
		List.setHead(list, 10) should be(List(10, 2, 3, 4))
	}
	behavior of "list.drop(n)"
	it should "drop the first `n` elements" in {
		val list = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		List.drop(list, 5) should be(List(6, 7, 8, 9, 10))
	}
	it should "return empty list when dropping from empty list" in {
		val list = List()
		List.drop(list, 5) should be(List())
	}
	it should "return empty list when dropping from list that is smaller than `n`" in {
		val list = List(1, 2, 3, 4)
		List.drop(list, 5) should be(List())
	}
	it should "return list if n < 0" in {
		val list = List(1, 2, 3, 4)
		List.drop(list, -1) should be(list)
	}
	behavior of "list.dropWhile(f)"
	it should "drop all elements matching predicate" in {
		val list = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
		List.dropWhile(list, (x: Int) => x <= 8) should be(List(9, 10))
	}
	it should "return empty list when applied to empty list" in {
		val list = List()
		List.dropWhile(list, (x: Int) => x <= 8) should be(List())
	}
	behavior of "list.init(l)"
	it should "return all but the last element of a list" in {
		val list = List(1, 2, 3, 4)
		List.init(list) should be(List(1, 2, 3))
	}
	it should "return empty list when given an empty list" in {
		List.init(List()) should be(List())
	}
	behavior of "list.length"
	it should "return length of list" in {
		List.length(List(1, 2, 3, 4)) should be(4)
	}
	it should "return length of empty list" in {
		List.length(List()) should be(0)
	}

	behavior of "list.lengthFoldRight"
	it should "return length of list" in {
		List.lengthFoldRight(List(1, 2, 3, 4)) should be(4)
	}
	it should "return length of empty list" in {
		List.lengthFoldRight(List()) should be(0)
	}

	behavior of "list.foldLeft"
	it should "fold left" in {
		val l = List(1, 2, 3, 4, 5)
		List.foldLeft(l, 0)(_ + _) should be(15)
	}
	it should "return init value on empty list" in {
		List.foldLeft(List[Int](), 0)(_ + _) should be(0)
	}
	behavior of "list.map"
	it should "map a function over a list" in {
		val l = List(1,2,3,4,5)
		List.map(l)(_ * 2) should be (List(2,4,6,8,10))
	}
	it should "return empty list if applied to empty list" in {
		List.map(List[Int]())(_ * 2) should be (List())
	}
	behavior of "list.reverse"
	it should "reverse a non-empty list" in {
		val l = List(1,2,3,4,5)
		List.reverse(l) should be (List(5,4,3,2,1))
	}
	it should "return empty list if given an empty list" in {
		List.reverse(List()) should be (List())
	}
	behavior of "foldRightWithLeft"
	it should "foldRight" in {
		val l = List(1,2,3,4,5)
		List.foldRightWithLeft(l,0)((a,acc) => acc + a) should be (15)
	}
	it should "foldRight with empty list" in {
		val l = List[Int]()
		List.foldRightWithLeft(l,0)((a,acc) => acc + a) should be (0)
	}
	behavior of "append"
	it should "append two non-empty lists" in {
		val l = List(1, 2, 3, 4, 5)
		val m = List(6, 7, 8, 9, 10)
		List.append2(l, m) should be(List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
	}
	it should "append one non-empty list" in {
		val l = List()
		val m = List(6, 7, 8, 9, 10)
		List.append2(l, m) should be(List(6, 7, 8, 9, 10))
	}
	it should "append two empty lists" in {
		val l = List()
		val m = List()
		List.append2(l, m) should be(List())
	}
	behavior of "concat"
	it should "concat a list of lists" in {
		val a = List(1,2,3)
		val b = List()
		val c = List(4,5,6)
		val l = List(a, b, c)
		List.concat(l) should be (List(1,2,3,4,5,6))
	}
	behavior of "add1"
	it should "add 1 to all elements of a list" in {
		val l = List(1,2,3,4)
		List.add1(l) should be (List(2,3,4,5))
	}
	it should "return empty list if applied to empty list" in {
		List.add1(List()) should be (List())
	}
	behavior of "filter"
	it should "filter any elements matching predicate" in {
		val l = List(1,2,3,4,5,6,7,8,9,10)
		List.filter(l)(_ % 2 == 0) should be (List(1,3,5,7,9))
	}
	it should "return empty list if applied to empty list" in {
		List.filter(List[Int]())(_ % 2 == 0) should be (List())
	}
	behavior of "flatmap"
	it should "concat a the lists returned by f" in {
		val l = List(1,2,3,4,5)
		List.flatMap(l)(a => List(a, a)) should be (List(1,1,2,2,3,3,4,4,5,5))
	}
	it should "return empty list if applied to empty list" in {
		List.flatMap(List[Int]())(a => List(a,a)) should be (List())
	}
	behavior of "addPairwise"
	it should "add two lists pairwise" in {
		val a = List(1,2,3,4)
		val b = List(2,3,4,5)
		List.addPairwise(a,b) should be (List(3,5,7,9))
	}
	it should "return empty if either param is empty" in {
		val l = List(1,2,3,4)
		List.addPairwise(List(),l) should be (List())
		List.addPairwise(l,List()) should be (List())
	}
	behavior of "zipwith"
	it should "zip two lists together, applying a f" in {
		val a = List(1,2,3,4)
		val b = List(2,3,4,5)
		List.zipWith(a, b)(_ + _) should be (List(3,5,7,9))
	}
	it should "return empty if either param is empty" in {
		val l = List(1,2,3,4)
		List.zipWith(List[Int](),l)(_ + _) should be (List())
		List.zipWith(l,List[Int]())(_ + _) should be (List())
	}
	it should "return result with length of first param even if other param is longer" in {
		val a = List(1,2,3,4)
		val b = List(2,3,4,5,6,7,8,9,10)
		List.length(List.zipWith(a, b)(_ + _)) should be (List.length(a))
	}

}
