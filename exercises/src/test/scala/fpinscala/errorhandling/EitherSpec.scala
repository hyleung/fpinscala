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
  behavior of "Either.flatMap"
  it should "follow the 'right' branch"  in {
    val r = Right("bar")
    val f = (v:String) => Right(s"hello $v")
    r flatMap  f should be (Right("hello bar"))
  }
  it should "follow the 'left' branch"  in {
    val l = Left("foo")
    val f = (v:String) => Right(s"hello $v")
    l flatMap f should be (l)
  }
  behavior of "Either.orElse"
  it should "return Right value" in {
    val r = Right("bar")
    val other = Right("foo")
    (r orElse other) should be (r)
  }
  it should "return other value" in {
    val l = Left("bar")
    val other = Right("foo")
    (l orElse other) should be (other)
  }
  behavior of "Either.map2"
  it should "Map two Rights" in {
    val r1 = Right("foo")
    val r2 = Right("bar")
    val result = r1.map2(r2)((s,b) => s"$s $b")
    result should be (Right("foo bar"))
  }
  it should "return first left" in {
    val left = Left("bar")
    val r1 = Right("foo")
    val result = left.map2(r1)((s,b) => s"$s $b")
    result should be (left)
  }
  it should "return second left" in {
    val r1 = Right("foo")
    val left = Left("bar")
    val result = r1.map2(left)((s,b) => s"$s $b")
    result should be (left)
  }

  behavior of "Either.traverse"
  it should "traverse and return Right" in {
    val list = List(2,4,6,8,10)
    val result = Either.traverse(list){a => if (a % 2 == 0) Right(a) else Left("I. can't. even.")}
    result should be (Right(list))
  }
  it should "traverse and return Left" in {
    val list = List(2,4,6,8,10,11)
    val result = Either.traverse(list){a => if (a % 2 == 0) Right(a) else Left("I. can't. even.")}
    result should be (Left("I. can't. even."))
  }

  behavior of "Either.sequence"
  it should "return list if all Right" in {
    val list = List(
      Right(1),
      Right(2),
      Right(3)
    )
    val result = Either.sequence(list)
    result should be (Right(List(1,2,3)))
  }
  it should "return Left if any Left" in {
    val list = List(
      Right(1),
      Left("yo dawg"),
      Right(3)
    )
    val result = Either.sequence(list)
    result should be (Left("yo dawg"))
  }
}
