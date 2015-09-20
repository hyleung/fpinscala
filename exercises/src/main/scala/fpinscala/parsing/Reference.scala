package fpinscala.parsing

import scala.util.matching.Regex

/**
 * Created with IntelliJ IDEA.
 * Date: 15-09-19
 * Time: 6:21 PM
 * To change this template use File | Settings | File Templates.
 */

object ReferenceTypes {

	/** A parser is a kind of state action that can fail. */
	type Parser[+A] = ParseState => Result[A]

	case class ParseState(loc: Location){}

	//Result types
	sealed trait Result[+A] {}
	case class Success[+A](get: A, length: Int) extends Result[A]
	case class Failure(get: ParseError, isCommitted: Boolean) extends Result[Nothing]
}

import fpinscala.parsing.ReferenceTypes.Parser

object Reference extends Parsers[Parser] {
	// so inner classes may call methods of trait
	override implicit def string(s: String): Parser[String] = ???

	override def flatMap[A, B](p: Parser[A])(f: (A) => Parser[B]): Parser[B] = ???

	override def or[A](s1: Parser[A], s2: Parser[A]): Parser[A] = ???

	override def succeed[A](a: A): Parser[A] = ???

	override def run[A](p: Parser[A])(input: String): Either[ParseError, A] = ???

	override implicit def regex(r: Regex): Parser[String] = ???
}