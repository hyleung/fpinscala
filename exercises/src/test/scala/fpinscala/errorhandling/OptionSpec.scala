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
    val result = option map {a => Some(s"$a")}
    result should be (Some("1"))
  }
  it should "map None to None" in {
    val option = None
    val result = option map {a => Some(s"$a")}
    result should be (None)
  }
}
