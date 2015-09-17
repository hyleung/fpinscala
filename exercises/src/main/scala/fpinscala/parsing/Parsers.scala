package fpinscala.parsing

import java.util.regex._
import scala.util.matching.Regex
import fpinscala.testing._
import fpinscala.testing.Prop._

trait Parsers[ParseError, Parser[+_]] { self => // so inner classes may call methods of trait
  def char(c:Char):Parser[Char] = string(c.toString).map(_.charAt(0))
  implicit def string(s:String):Parser[String]
  implicit def operators[A](p:Parser[A]): ParserOps[A] = ParserOps[A](p)
  implicit def asStringParser[A](a:A)(implicit f: A => Parser[String]):ParserOps[String] = ParserOps(f(a))
  def or[A](s1: Parser[A], s2: Parser[A]):Parser[A] = ???
  def listofN[A](n: Int, p:Parser[A]):Parser[List[A]] = ???
  def many[A](p:Parser[A]):Parser[List[A]] = ???
  def succeed[A](a:A):Parser[A] = string("").map(_ => a)
  def map[A,B](a:Parser[A])(f: A => B):Parser[B] = ???
  def slice[A](p:Parser[A]):Parser[String] = ???
  def many1[A](p:Parser[A]):Parser[List[A]] = ???
  def product[A,B](p1:Parser[A], p2:Parser[B]):Parser[(A,B)]

  def map2[A,B,C](p1:Parser[A],p2:Parser[B])(f: (A,B) => C):Parser[C] = product(p1,p2).map{ case (a,b) => f(a,b) }

  val numA:Parser[Int] = char('a').many.map(_.size)

  def run[A](p: Parser[A])(input: String):Either[ParseError,A] = ???
  case class ParserOps[A](p: Parser[A]) {
    def | [B>:A](p2:Parser[B]) = self.or(p,p2)
    def or [B>:A](p2:Parser[B]) = self.or(p,p2)
    def many [B>:A]:Parser[List[B]] = self.many(p)
    def map [B>:A,C](f: B => C):Parser[C] = self.map(p)(f)
    def ** [B>:A](p2:Parser[B]):Parser[(A,B)] = self.product(p,p2)
  }

  object Laws {
    def equal[A](p1:Parser[A], p2:Parser[A])(in:Gen[String]):Prop =
      forAll(in)(s => run(p1)(s) == run(p2)(s))
    def mapLaw[A](p:Parser[A])(in:Gen[String]):Prop =
      equal(p, p.map(a => a))(in)
    def succeedLaw[A](in:Gen[String]):Prop =
      forAll(in)(s => run(succeed("a"))(s) == Right("a"))
    def productAssociativityLaw[A,B,C](p1:Parser[A],p2:Parser[B],p3:Parser[C])(in:Gen[String]):Prop =
      equal(product(p1,p2),product(p2,p3))(in)

  }
}

case class Location(input: String, offset: Int = 0) {

  lazy val line = input.slice(0,offset+1).count(_ == '\n') + 1
  lazy val col = input.slice(0,offset+1).reverse.indexOf('\n')

  def toError(msg: String): ParseError =
    ParseError(List((this, msg)))

  def advanceBy(n: Int) = copy(offset = offset+n)

  /* Returns the line corresponding to this location */
  def currentLine: String = 
    if (input.length > 1) input.lines.drop(line-1).next
    else ""
}

case class ParseError(stack: List[(Location,String)] = List(),
                      otherFailures: List[ParseError] = List()) {
}