package fpinscala.parsing

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-19
 * Time: 6:32 PM
 * To change this template use File | Settings | File Templates.
 */
class ReferenceSpec extends FlatSpec with Matchers{
	import Reference._
	import Reference.{run => r}
	behavior of "success"
	it should "always return the value" in {
		val p = succeed("a")
		val result: Either[ParseError, String] = r(p)("blah blah blah")
		result should be (Right("a"))
	}
	behavior of "string"
	it should "return success" in {
		val p = string("abra")
		val result = r(p)("abra cadabra")
		result should be (Right("abra"))
	}
	it should "return failure" in {
		val p = string("abra")
		val result = r(p)("aAbra cadAbra")
		result should be (Left(ParseError(
			List((Location("aAbra cadAbra",1),"abra")))))
	}
	it should "return failure if search string is longer" in {
		val p = string("abracadabra")
		val result = r(p)("abra")
		result should be (Left(ParseError(
			List((Location("abra",4),"abracadabra")))))
	}
}
