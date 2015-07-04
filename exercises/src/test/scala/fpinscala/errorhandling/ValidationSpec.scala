package fpinscala.errorhandling

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-07-03
 * Time: 5:42 PM
 * To change this template use File | Settings | File Templates.
 */
class ValidationSpec extends FlatSpec with Matchers{
	behavior of "mkPerson"
	it should "return a Person if both name and age are valid" in {
		mkPerson("John","20") should be (Right(Person(Name("John"),Age(20))))
	}
	it should "return a error if name is invalid" in {
		mkPerson("","20") should be (Left("error"))
	}
	it should "return an error if age is invalid" in {
		mkPerson("John","twenty") should be (Left("parse error"))
	}
	behavior of "mkPersonAllErrors"
	it should "return a Person if both name and age are valid" in {
		mkPersonAllErrors("John","20") should be (Right(Person(Name("John"),Age(20))))
	}
	it should "return a error if name is invalid" in {
		mkPersonAllErrors("","20") should be (Left(List("error")))
	}
	it should "return an error if age is invalid" in {
		mkPersonAllErrors("John","twenty") should be (Left(List("parse error")))
	}
	it should "return all errors if age and name are invalid" in {
		mkPersonAllErrors("","twenty") should be (Left(List("error","parse error")))
	}

	def mkPerson(name:String, age:String):Either[String,Person] = mkName(name).map2(mkAge(age))((name,age) => Person(name,age))
	def mkPersonAllErrors(name:String, age:String):Either[List[String],Person] = mkName(name).map3(mkAge(age))((name,age) => Person(name,age))
	def mkName(s:String):Either[String,Name] = if (s != null && !s.isEmpty ) Right(Name(s)) else Left("error")
	def mkAge(s:String):Either[String,Age] = {
		try {
			Right(Age(Integer.parseInt(s)))
		} catch {
			case t:NumberFormatException => Left("parse error")
		}
	}
}



case class Person(name:Name, age:Age)
sealed case class Name(n:String)
sealed case class Age(age:Int)