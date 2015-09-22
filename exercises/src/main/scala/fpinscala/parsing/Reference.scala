package fpinscala.parsing

import fpinscala.parsing.ReferenceTypes.{Failure, ParseState, Success}

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
	override implicit def string(a: String): Parser[String] = s => {
		val idx = findMismatchIndex(s.loc.input, a, s.loc.offset)
		if (idx == -1) {
			Success(a, a.length)
		} else {
			val newLocation = Location(s.loc.input, s.loc.offset + idx)
			Failure(ParseError(List((newLocation,a))), isCommitted = true)
		}

	}

	override implicit def regex(r: Regex): Parser[String] = s => {
		r.findPrefixMatchOf(s.loc.input) match {
			case Some(m) => Success(m.matched,m.end - m.start)
			case None => Failure(ParseError(List((Location(s.loc.input,0),r.toString()))), isCommitted = true)
		}
	}

	override def flatMap[A, B](p: Parser[A])(f: (A) => Parser[B]): Parser[B] = s => {
		???
	}

	override def or[A](s1: Parser[A], s2: Parser[A]): Parser[A] = s => {
		s1(s) match {
			case Success(a,l) => Success(a,l)
			case Failure(e1,e1Committed) => s2(s) match {
				case Success(a,l) => Success(a,l)
				case Failure(e2, e2Committed) => {
						val location = Location(s.loc.input,0)
						val errors = e1.stack ++ e2.stack
						Failure(ParseError(errors), isCommitted = true)
					}
			}
		}
	}

	override def succeed[A](a: A): Parser[A] = s => Success(a,0)

	override def run[A](p: Parser[A])(input: String): Either[ParseError, A] =  {
		val state = ParseState(Location(input))
		p(state) match  {
			case Success(a,l) => Right(a)
			case Failure(error, committed) => Left(error)
		}
	}

	/** Returns -1 if s1.startsWith(s2), otherwise returns the
	  * first index where the two strings differed. If s2 is
	  * longer than s1, returns s1.length. */
	def findMismatchIndex(input:String, search:String, offset:Int): Int = {
		if (search.length > input.length) {
			input.length
		} else {
			var i = 0
			while( i < input.length && i < search.length) {
				if(input.charAt(i) != search.charAt(i)) return i
				i +=1
			}
			-1
		}
	}
}