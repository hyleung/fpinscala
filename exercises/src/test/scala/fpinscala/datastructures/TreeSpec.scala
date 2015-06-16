package fpinscala.datastructures

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created by hyleung on 15-06-16.
 */
class TreeSpec extends FlatSpec with Matchers{
  behavior of "tree.size"
  it should "compute the size of the single level tree" in {
    val tree = Branch(Leaf(1),Leaf(2))
    Tree.size(tree) should be (2)
  }
  it should "compute the size of the mutlti-level tree" in {
    val tree = Branch(Leaf(1),Branch(Leaf(2),Leaf(3)))
    Tree.size(tree) should be (3)
  }

}
