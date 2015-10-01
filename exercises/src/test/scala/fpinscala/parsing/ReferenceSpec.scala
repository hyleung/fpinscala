package fpinscala.parsing

import fpinscala.parsing.ReferenceTypes.{Success, ParseState}
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
	it should "return success with correct length" in {
		val p = string("abra")
		val result = p(ParseState(Location("abracadabra")))
		result should be (Success("abra",4))
	}
	it should "return failure" in {
		val p = string("abra")
		val result = r(p)("aAbra cadAbra")
		result should be (Left(ParseError(
			List((Location("aAbra cadAbra",1),"'abra'")))))
	}
	it should "return failure if search string is longer" in {
		val p = string("abracadabra")
		val result = r(p)("abra")
		result should be (Left(ParseError(
			List((Location("abra",4),"'abracadabra'")))))
	}
	behavior of "regex"
	it should "return success" in {
		val p = Reference.regex("[\\d]+".r)
		val result = r(p)("12345")
		result should be (Right("12345"))
	}
	it should "return failure" in {
		val p = Reference.regex("[\\d]+".r)
		val result = r(p)("abcde")
		result should be (Left(ParseError(List((Location("abcde",0),"[\\d]+")))))
	}

	behavior of "or"
	it should "return success on first parser" in {
		val p1 = string("hello")
		val p2 = string("hey,")
		val result = r(p1 or p2)("hello parser")
		result should be (Right("hello"))
	}
	it should "return success on second parser" in {
		val p1 = string("hello")
		val p2 = string("hey,")
		val result = r(p1 or p2)("hey, parser")
		result should be (Right("hey,"))
	}
	it should "return failure on first parser" in {
		val p1 = string("hello")
		val p2 = string("hey,")
		val result = r(p1 or p2)("blah parser")
		result should be (Left(ParseError(List((Location("blah parser",0),"'hello'"),(Location("blah parser",0),"'hey,'")))))
	}

	behavior of "many"
	it should "match many" in {
		val result = r(char('a').many)("aaaaa")
		result should be (Right(List('a','a','a','a','a')))
	}

	behavior of "flatMap"
	it should "chain multiple parsers" in {
		val p = string("hello").flatMap(s => map2(succeed(s),string("world!"))((a,b) => b + a))
		val result = r(p)("helloworld!")
		result should be (Right("world!hello"))
	}
	it should "work with for comprehensions" in {
		val p = for {
			a <- string("hello")
			b <- string(" ")
			c <- string("world")
		} yield (a,b,c)
		val result = r(p)("hello world")
		result should be (Right(("hello"," ", "world")))
	}
	it should "return failure" in {
		val p = string("hello").flatMap(s => succeed(s))
		val result = r(p)("HELLO world")
		result should be (Left(ParseError(List((Location("HELLO world",0),"'hello'")))))
	}

	behavior of "product"
	it should "return pair" in {
		val p = "hello" ** "world"
		val result = r(p)("helloworld")
		result should be(Right("hello", "world"))
	}
	behavior of "double"
	it should "return a double on successful parsing" in {
		val result = r(double)("3.14")
		result should be (Right(3.14))
	}
	it should "return failure if not a number" in {
		val result = r(double)("hello")
		result should be (Left(ParseError(List((Location("hello",0),"[0-9]+[.]{1}[0-9]+")))))
	}

	behavior of "integer"
	it should "return a integer on successful parsing" in {
		val result = r(integer)("3")
		result should be (Right(3))
	}
	it should "return failure if not a number" in {
		val result = r(integer)("hello")
		result should be (Left(ParseError(List((Location("hello",0),"[0-9]+")))))
	}

	behavior of "parseBoolean"
	it should "return true" in {
		val result = r(parseBoolean)("true")
		result should be (Right(true))
	}
	it should "return false" in {
		val result = r(parseBoolean)("false")
		result should be (Right(false))
	}
	it should "return failure" in {
		val result = r(parseBoolean)("blargh")
		result should be (Left(ParseError(List((Location("blargh",0),"'true'"),(Location("blargh",0),"'false'")))))
	}

	behavior of "whitespace"
	it should "parse spaces" in {
		val result = r(whitespace)("     ")
		result should be (Right(""))
	}
	it should "parse line breaks" in {
		val result = r(whitespace)("\n")
		result should be (Right(""))
	}
	it should "parse tabs" in {
		val result = r(whitespace)("\t")
		result should be (Right(""))
	}
	it should "parse mix" in {
		val result = r(whitespace)("\t   \n\t\t   ")
		result should be (Right(""))
	}
	it should "return failure" in {
		val result = r(whitespace)("abc")
		result should be (Left(ParseError(List((Location("abc",0),"whitespace not found")))))
	}

	behavior of "quotedString"
	it should "parse quoted string with double quotes" in {
		val result = r(quotedString)("\"hello world\"")
		result should be (Right("hello world"))
	}
	it should "parse quoted string with single quotes" in {
		val result = r(quotedString)("'hello world'")
		result should be (Right("hello world"))
	}
	it should "return failure" in {
		val result = r(quotedString)("hello world")
		result match {
			case r@Right(_) => fail(s"Expecting Left result but got $r")
			case Left(_) => //pass
		}
	}

	behavior of "label"
	it should "return expected label on failure" in {
		val result = r(integer label "ruh-roh")("abc")
		result should be (Left(ParseError(List((Location("abc",0),"ruh-roh")))))
	}

	behavior of "scope"
	it should "add information to failures" in {
		val result = r("def" scope "ruh-roh")("abc")
		result should be (Left(ParseError(List((Location("abc",0),"ruh-roh: 'def'")))))
	}

	behavior of "slice"
	it should "return input if parser matches" in {
		val input = "abc"
		val result = r(slice("ab"))(input)
		result should be (Right("ab"))
	}
	it should "match example in book" in {
		val input = "aaba"
		val p = slice((char('a')|char('b')).many)
		val result = r(p)(input)
		result should be (Right("aaba"))
	}

	behavior of "skipL"
	it should "return result of second parser" in {
		val p = skipL("abcde", "fgh")
		val result = r(p)("abcdefgh")
		result should be (Right("fgh"))
	}
	it should "return result of second parser with operator" in {
		val p = "abcde" *> "fgh"
		val result = r(p)("abcdefgh")
		result should be (Right("fgh"))
	}

	behavior of "skipR"
	it should "return result of first parser" in {
		val p = skipR("abcde", "fgh")
		val result = r(p)("abcdefgh")
		result should be (Right("abcde"))
	}
	it should "return result of first parser with operator" in {
		val p = "abcde" <* "fgh"
		val result = r(p)("abcdefgh")
		result should be (Right("abcde"))
	}

	behavior of "surround"
	it should "return just the middle" in {
		val p = surround("hello")("{","}")
		val result = r(p)("{hello}")
		result should be (Right("hello"))
	}
	it should "return just the middle with operator" in {
		val p = "hello".surroundWith("{","}")
		val result = r(p)("{hello}")
		result should be (Right("hello"))
	}

	behavior of "magic spell or gibberish"
	it should "parse magic" in {
		val spaces = " ".many
		val p1 = scope("magic spell") {
			"abra" ** spaces ** "cadabra"
		}
		val result = r(p1)("abra cadabra")
		result should be (Right(("abra",List(" ")), "cadabra"))
	}
}
