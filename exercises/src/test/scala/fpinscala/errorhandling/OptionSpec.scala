package fpinscala.errorhandling

import org.scalatest.{Matchers, FlatSpec}

import scala.{Option => _, Some => _, Either => _, _} // hide std library `Option`, `Some` and `Either`, since we are writing our own in this chapter
import grizzled.math.stats._
import org.scalacheck.Prop.forAll
/**
 * Created by hyleung on 15-06-22.
 */
class OptionSpec extends FlatSpec with Matchers {
  behavior of "Option.map"
  it should "transform Option of type A to Option of type B" in {
    val option = Some(1)
    val result = option map {a => s"$a"}
    result should be (Some("1"))
  }
  it should "map None to None" in {
    val option = None
    val result = option map {a => s"$a"}
    result should be (None)
  }
  behavior of "Option.flatmap"
  it should "apply with Some" in {
    val option = Some(1)
    val result = option flatMap { a => Some(a + 1)}
    result should be (Some(2))
  }
  it should "apply with None" in {
    val option:Option[Int]  = None
    val result = option flatMap { a => Some(a + 1)}
    result should be (None)
  }
  behavior of "Option map/flatmap"
  it should "allow composition with Some" in {
    val optionA = Some(1)
    val optionB = Some(2)
    val optionC = Some(3)
    val result = optionA flatMap {
      a => optionB flatMap {
        b => optionC map {c => a + b + c}}
    }
    result should be (Some(6))
  }
  it should "allow composition with None" in {
    val optionA = Some(1)
    val optionB:Option[Int] = None
    val result = optionA flatMap {
      a => optionB map {
        b => a + b}
    }
    result should be (None)
  }
  it should "compose using for comprehensions for Some" in {
    val optionA = Some(1)
    val optionB = Some(2)
    val optionC = Some(3)
    for {
      a <- optionA
      b <- optionB
      c <- optionC
    } yield a + b + c should be (6)
  }
  it should "compose using for comprehensions for None" in {
    val optionA = Some(1)
    val optionB = Some(2)
    val optionC: Option[Int] = None
    for {
      a <- optionA
      b <- optionB
      c <- optionC
    } yield a + b + c should be (None)
  }

  behavior of "Option.getOrElse"
  it should "return the default value for None" in {
    val option:Option[Int] = None
    val result = option.getOrElse(42)
    result should be (42)
  }
  it should "return the value for Some" in {
    val option = Some(1)
    val result = option.getOrElse(42)
    result should be (1)
  }

  behavior of "Option.orElse"
  it should "return value if available" in {
    val option = Some(1)
    val result = option orElse Some(2)
    result should be (Some(1))
  }
  it should "return fallback if not available" in {
    val option:Option[Int] = None
    val result = option orElse Some(2)
    result should be (Some(2))
  }

  behavior of "Option.filter"
  it should "return Some if predicate applies" in {
    val option = Some(42)
    val result = option filter {_ % 2 == 0}
    result should be (Some(42))
  }
  it should "return None if predicate applies" in {
    val option = Some(43)
    val result = option filter {_ % 2 == 0}
    result should be (None)
  }
  it should "return None if applied to None" in {
    val option:Option[Int] = None
    val result = option filter {_ % 2 == 0}
    result should be (None)
  }

  behavior of "Option.variance"
  it should "compute the variance of Some" in {
    val seq:Seq[Double] = Seq(1,2,3,4)
    val result: Option[Double] = Option.variance(seq)
    result should be (Some(populationVariance(seq:_*)))
  }
  it should "return None for varance of empty seq" in {
    Option.variance(Seq.empty[Double]) should be (None)
  }

  behavior of "Option.map2"
  it should "perform computation on value of each" in {
    val optionA = Some(1)
    val optionB = Some(1)
    Option.map2(optionA, optionB)((a,b) => a + b) should be (Some(2))
  }
  it should "return None" in {
    val optionA = Some(1)
    val optionB:Option[Int] = None
    Option.map2(optionA, optionB)((a,b) => a + b) should be (None)
  }

  behavior of "Option.sequence"
  it should "convert a list of option into list" in {
    val options = List(Some(1), Some(2), Some(3))
    Option.sequence(options) should be (Some(List(1,2,3)))
  }
  it should "convert a list of option containing None into list" in {
    val options = List(Some(1), None, Some(3))
    Option.sequence(options) should be (None)
  }
  it should "return Some(Nil) for empty list" in {
    Option.sequence(List.empty[Option[Int]]) should be (Some(Nil))
  }

}
