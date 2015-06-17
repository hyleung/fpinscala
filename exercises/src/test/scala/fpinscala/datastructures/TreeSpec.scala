package fpinscala.datastructures

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by hyleung on 15-06-16.
 */
class TreeSpec extends FlatSpec with Matchers {
	behavior of "tree.size"
	it should "compute the size of the single level tree" in {
		val tree = Branch(Leaf(1), Leaf(2))
		Tree.size(tree) should be(2)
	}
	it should "compute the size of the mutlti-level tree" in {
		val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
		Tree.size(tree) should be(3)
	}
	behavior of "tree.maximum"
	it should "determine the maximum in a tree" in {
		val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
		Tree.maximum(tree) should be(3)
	}
	it should "determine maximum in a singleton tree" in {
		val tree = Leaf(10)
		Tree.maximum(tree) should be(10)
	}
	it should "determine maximum in deep unbalanced tree" in {
		val tree = Branch(Leaf(100), Branch(Branch(Leaf(1), Leaf(2)), Branch(Leaf(3), Branch(Leaf(4), Leaf(5)))))
		Tree.maximum(tree) should be(100)
	}
	it should "determine maximum in deep unbalanced tree (max in deep leaf)" in {
		val tree = Branch(Leaf(100), Branch(Branch(Leaf(1), Leaf(2)), Branch(Leaf(3), Branch(Leaf(4), Leaf(500)))))
		Tree.maximum(tree) should be(500)
	}
	behavior of "tree.depth"
	it should "compute depth of singleton tree" in {
		val tree = Leaf(1)
		Tree.depth(tree) should be (1)
	}
	it should "compute depth of branch" in {
		val tree = Branch(Leaf(1),Leaf(2))
		Tree.depth(tree) should be (1)
	}

	it should "compute the depth of a deeper tree" in {
		val tree = Branch(
							Leaf(1),
							Branch(
								Branch(Leaf(1), Leaf(2)),
								Branch(Leaf(3), Branch(
														Leaf(4),
														Leaf(5)))))
		Tree.depth(tree) should be(3)
	}
}
