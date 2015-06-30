package fpinscala.errorhandling

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by hyleung on 15-06-30.
 */
class EitherSpec extends FlatSpec with Matchers{
  behavior of "Either.map"
  it should "follow the 'right' branch"  in {
    val r = Right("bar")
    val f = (v:String) => s"hello $v"
    r map f should be (Right("hello bar"))
  }
  it should "follow the 'left' branch"  in {
    val l = Left("foo")
    val f = (v:String) => s"hello $v"
    l map f should be (l)
  }
}
