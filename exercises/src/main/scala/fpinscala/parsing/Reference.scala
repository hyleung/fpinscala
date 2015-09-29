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

	case class ParseState(loc: Location){
		/* return a new state with location advanced by numChars */
		def advanceBy(numChars: Int): ParseState =
			copy(loc = loc.copy(offset = loc.offset + numChars))
		def slice(n: Int) = loc.input.substring(loc.offset, loc.offset + n)
	}

	//Result types
	sealed trait Result[+A] {
		/* Used by `scope`, `label`. */
		def mapError(f: ParseError => ParseError): Result[A] = this match {
			case Failure(e,c) => Failure(f(e),c)
			case _ => this
		}
		def advanceSuccess(n: Int): Result[A] = this match {
			case Success(a,m) => Success(a,n+m)
			case _ => this
		}
		/* Used by `flatMap` */
		def addCommit(isCommitted: Boolean): Result[A] = this match {
			case Failure(e,c) => Failure(e, c || isCommitted)
			case _ => this
		}
	}
	case class Success[+A](get: A, length: Int) extends Result[A]
	case class Failure(get: ParseError, isCommitted: Boolean) extends Result[Nothing]
}

import fpinscala.parsing.ReferenceTypes.Parser

object Reference extends Parsers[Parser] {
	// so inner classes may call methods of trait
	override implicit def string(a: String): Parser[String] = s => {
		val msg = s"'$a'"
		val idx = findMismatchIndex(s.loc.input, a, s.loc.offset)
		if (idx == -1) {
			Success(a, a.length)
		} else {
			val newLocation = Location(s.loc.input, s.loc.offset + idx)
			Failure(ParseError(List((newLocation,a))).label(msg), isCommitted = true)
		}

	}

	override implicit def regex(r: Regex): Parser[String] = s => {
		r.findPrefixMatchOf(s.loc.input) match {
			case Some(m) => Success(m.matched,m.end - m.start)
			case None => Failure(ParseError(List((Location(s.loc.input,0),r.toString()))), isCommitted = false)
		}
	}

	override def flatMap[A, B](p: Parser[A])(f: (A) => Parser[B]): Parser[B] = s => {
		p(s) match {
			case Success(a,l) => f(a)(s.advanceBy(l)).addCommit(l !=0).advanceSuccess(l)
			case fail:Failure => fail
		}
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

	def label[A](msg: String)(p: Parser[A]): Parser[A] = s => {
		p(s) match {
			case s@Success(_,_) => s
			case f@Failure(_,committed) => f.mapError(err => err.label(msg))
		}
	}

	def scope[A](msg: String)(p:Parser[A]):Parser[A] = s => {
		p(s) match {
			case s@Success(_,_) => s
			case f@Failure(_,committed) => f.mapError(err => err.scope(msg))
		}
	}

	override def run[A](p: Parser[A])(input: String): Either[ParseError, A] =  {
		val state = ParseState(Location(input))
		p(state) match  {
			case Success(a,l) => Right(a)
			case Failure(error, committed) => Left(error)
		}
	}

	def slice[A](p: Parser[A]): Parser[String] = s => {
		p(s) match {
			case Success(a,l) => Success(s.slice(l), l)
			case f@Failure(_,_) => f
		}
	}

	/** Returns -1 if s1.startsWith(s2), otherwise returns the
	  * first index where the two strings differed. If s2 is
	  * longer than s1, returns s1.length. */
	def findMismatchIndex(s1:String, s2:String, offset:Int): Int = {
		var i = 0
		while (i + offset < s1.length && i < s2.length) {
			if (s1.charAt(i+offset) != s2.charAt(i)) return i
			i += 1
		}
		if (s1.length-offset >= s2.length) -1
		else s1.length-offset
	}
}
