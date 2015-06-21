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
	it should "compute depth of singleton tree (just a root node)" in {
		val tree = Leaf(1)
		Tree.depth(tree) should be (0)
	}
	it should "compute depth of branch" in {
		val tree = Branch(Leaf(1),Leaf(2))
		Tree.depth(tree) should be (1)
	}

	it should "compute the depth of a deeper tree" in {
		//			 0 -------1------2-----------3-------4
		val tree = Branch(
							Leaf(1),
							Branch(
								Branch(Leaf(1), Leaf(2)),
								Branch(Leaf(3), Branch(
														Leaf(4),
														Leaf(5)))))
		Tree.depth(tree) should be(4)
	}
	behavior of "equals"
	it should "evaluate true" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))

		val expected = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))

		tree should be (expected)
	}
	it should "evaluate false" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))

		val expected = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(10)))))

		tree should not be (expected)
	}

	behavior of "map"
	it should "map a function over singleton" in {
		val tree = Leaf(1)
		Tree.map(tree)(_ * 10) should be (Leaf(10))
	}
	it should "map a function over branch" in {
		val tree = Branch(Leaf(1),Leaf(1))
		Tree.map(tree)(_ * 10) should be (Branch(Leaf(10),Leaf(10)))
	}
	it should "map a function over deeper tree" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))

		val expected = Branch(
			Leaf(10),
			Branch(
				Branch(Leaf(10), Leaf(10)),
				Branch(Leaf(10), Branch(
					Leaf(10),
					Leaf(10)))))

		Tree.map(tree)(_ * 10) should be (expected)
	}
	behavior of "fold"
	it should "apply over a singleton tree" in {
		val tree = Leaf(1)
		Tree.fold(tree)((a:Int) => a)( (b,c) => b + c) should be (1)
	}
	it should "apply over a branch tree" in {
		val tree = Branch(Leaf(1),Leaf(1))
		Tree.fold(tree)((a:Int) => a)( (b,c) => b + c) should be (2)
	}
	it should "apply over deeper tree" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))
		Tree.fold(tree)((a:Int) => a)( (b,c) => b + c) should be (6)
	}

	it should "apply over deeper tree with some function" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))
		Tree.fold(tree)((a:Int) => a * 2)( (b,c) => b + c) should be (12)
	}

	behavior of "mapFold"
	it should "map a function over singleton" in {
		val tree = Leaf(1)
		Tree.mapFold(tree)(_ * 10) should be (Leaf(10))
	}
	it should "map a function over branch" in {
		val tree = Branch(Leaf(1),Leaf(1))
		Tree.mapFold(tree)(_ * 10) should be (Branch(Leaf(10),Leaf(10)))
	}
	it should "map a function over deeper tree" in {
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(1)),
				Branch(Leaf(1), Branch(
					Leaf(1),
					Leaf(1)))))

		val expected = Branch(
			Leaf(10),
			Branch(
				Branch(Leaf(10), Leaf(10)),
				Branch(Leaf(10), Branch(
					Leaf(10),
					Leaf(10)))))

		Tree.mapFold(tree)(_ * 10) should be (expected)
	}

	behavior of "tree.depthFold"
	it should "compute depth of singleton tree (just a root node)" in {
		val tree = Leaf(1)
		Tree.depthFold(tree) should be (0)
	}
	it should "compute depth of branch" in {
		val tree = Branch(Leaf(1),Leaf(2))
		Tree.depthFold(tree) should be (1)
	}

	it should "compute the depth of a deeper tree" in {
		//			 0 -------1------2-----------3-------4
		val tree = Branch(
			Leaf(1),
			Branch(
				Branch(Leaf(1), Leaf(2)),
				Branch(Leaf(3), Branch(
					Leaf(4),
					Leaf(5)))))
		Tree.depthFold(tree) should be(4)
	}

	behavior of "tree.maximumFold"
	it should "determine the maximum in a tree" in {
		val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
		Tree.maximumFold(tree) should be(3)
	}
	it should "determine maximum in a singleton tree" in {
		val tree = Leaf(10)
		Tree.maximumFold(tree) should be(10)
	}
	it should "determine maximum in deep unbalanced tree" in {
		val tree = Branch(Leaf(100), Branch(Branch(Leaf(1), Leaf(2)), Branch(Leaf(3), Branch(Leaf(4), Leaf(5)))))
		Tree.maximumFold(tree) should be(100)
	}
	it should "determine maximum in deep unbalanced tree (max in deep leaf)" in {
		val tree = Branch(Leaf(100), Branch(Branch(Leaf(1), Leaf(2)), Branch(Leaf(3), Branch(Leaf(4), Leaf(500)))))
		Tree.maximumFold(tree) should be(500)
	}

	behavior of "tree.sizeFold"
	it should "compute the size of the single level tree" in {
		val tree = Branch(Leaf(1), Leaf(2))
		Tree.sizeFold(tree) should be(2)
	}
	it should "compute the size of the mutlti-level tree" in {
		val tree = Branch(Leaf(1), Branch(Leaf(2), Leaf(3)))
		Tree.sizeFold(tree) should be(3)
	}
}
