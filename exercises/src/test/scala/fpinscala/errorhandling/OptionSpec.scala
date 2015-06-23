package fpinscala.errorhandling

import org.scalatest.{Matchers, FlatSpec}
import scala.{Option => _, Some => _, Either => _, _} // hide std library `Option`, `Some` and `Either`, since we are writing our own in this chapter
/**
 * Created by hyleung on 15-06-22.
 */
class OptionSpec extends FlatSpec with Matchers{
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
}
