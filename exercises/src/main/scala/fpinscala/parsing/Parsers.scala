package fpinscala.parsing

import java.util.regex._
import fpinscala.parsing.ReferenceTypes.Failure

import scala.util.matching.Regex
import fpinscala.testing._
import fpinscala.testing.Prop._

trait Parsers[Parser[+_]] { self => // so inner classes may call methods of trait
  //primitives
  implicit def string(s:String):Parser[String]
  implicit def regex(r:Regex):Parser[String]
  /**
   * Returns the portion of input inspected by p if successful
   */
  def slice[A](p:Parser[A]):Parser[String]
  def flatMap[A,B](p: Parser[A])(f: A => Parser[B]):Parser[B]
  def or[A](s1: Parser[A], s2: Parser[A]):Parser[A]
  def succeed[A](a:A):Parser[A]
  def label[A](msg: String)(p: Parser[A]): Parser[A]
  def scope[A](msg: String)(p: Parser[A]): Parser[A]

  def run[A](p: Parser[A])(input: String):Either[ParseError,A]

  //combinators
  /** Sequences two parsers, ignoring the result of the first.
    * We wrap the ignored half in slice, since we don't care about its result. */
  def skipL[B](p: Parser[Any], p2: => Parser[B]): Parser[B] = map2(slice(p), p2)((_,b) => b)

  /** Sequences two parsers, ignoring the result of the second.
    * We wrap the ignored half in slice, since we don't care about its result. */
  def skipR[A](p: Parser[A], p2: => Parser[Any]): Parser[A] = map2(p, slice(p2))((a,_) => a)

  def char(c:Char):Parser[Char] = string(c.toString).map(_.charAt(0))

  def listOfN[A](n: Int, p:Parser[A]):Parser[List[A]] = map2(p,listOfN(n -1, p))(_ :: _) | succeed(List())

  def many[A](p:Parser[A]):Parser[List[A]] = map2(p, many(p))(_ :: _) | succeed(List())

  def map[A,B](pa:Parser[A])(f: A => B):Parser[B] = pa.flatMap(a => succeed(f(a)))

  def many1[A](p:Parser[A]):Parser[List[A]] = map2(p, many(p))(_ :: _)

  def product[A,B](p1:Parser[A], p2:Parser[B]):Parser[(A,B)] = for {
    a <- p1
    b <- p2
  } yield (a,b)

  def map2[A,B,C](p1:Parser[A],p2: => Parser[B])(f: (A,B) => C):Parser[C] = for {
    a <- p1
    b <- p2
  } yield  f(a,b)

  val numA:Parser[Int] = char('a').many.map(_.size)

  def nChars(c:Char):Parser[Int] = for {
    d <- "[0-9]+".r
    n = d.toInt //<-- didn't know you could do this!
    _ <- listOfN(n,char(c))
  } yield n

  def double:Parser[Double] =  "[0-9]+[.]{1}[0-9]+".r.map(s => s.toDouble)

  def integer:Parser[Int] = "[0-9]+".r.map(s => s.toInt)

  def parseBoolean:Parser[Boolean] = (string("true") | string("false")).map(s => s.toBoolean)

  def whitespace:Parser[String] = ("[\\s\\t\\n]+".r label "whitespace not found").map(s => "")

  def quotedString:Parser[String] = doubleQuotedString | singleQuotedString label "quoted string"

  def singleQuotedString: Parser[String] = {
    "[\\\']{1}[\\w\\s]+[\\\']{1}".r
        .map(s => s.replace("\'", ""))
  }

  def doubleQuotedString: Parser[String] = {
    "[\\\"]{1}[\\w\\s]+[\\\"]{1}".r.map(s => s.replace("\"", ""))
  }

  def surround[A](p:Parser[A])(start:Parser[Any], end:Parser[Any]):Parser[A] = start *> p <* end

  implicit def operators[A](p:Parser[A]): ParserOps[A] = ParserOps[A](p)
  implicit def asStringParser[A](a:A)(implicit f: A => Parser[String]):ParserOps[String] = ParserOps(f(a))

  case class ParserOps[A](p: Parser[A]) {
    def | [B>:A](p2:Parser[B]) = self.or(p,p2)
    def or [B>:A](p2:Parser[B]) = self.or(p,p2)
    def many [B>:A]:Parser[List[B]] = self.many(p)
    def map [B](f: A => B):Parser[B] = self.map(p)(f)
    def ** [B>:A](p2:Parser[B]):Parser[(A,B)] = self.product(p,p2)
    def flatMap [B](f: A => Parser[B]):Parser[B] = self.flatMap(p)(f)
    def label(msg:String):Parser[A] = self.label(msg)(p)
    def scope(msg:String):Parser[A] = self.scope(msg)(p)
    def *>[B](p2: => Parser[B]) = self.skipL(p, p2)
    def <*(p2: => Parser[Any]) = self.skipR(p, p2)
    def surroundWith(start:Parser[Any], end:Parser[Any]):Parser[A] = self.surround(p)(start, end);
  }

  object Laws {
    def equal[A](p1:Parser[A], p2:Parser[A])(in:Gen[String]):Prop =
      forAll(in)(s => run(p1)(s) == run(p2)(s))
    def mapLaw[A](p:Parser[A])(in:Gen[String]):Prop =
      equal(p, p.map(a => a))(in)
    def succeedLaw[A](in:Gen[String]):Prop =
      forAll(in)(s => run(succeed("a"))(s) == Right("a"))

    // (a * b) * c == a * (b * c)
    def productAssociativityLaw[A,B,C](p1:Parser[A],p2:Parser[B],p3:Parser[C])(in:Gen[String]):Prop =
      equal(product(p1,p2),product(p2,p3))(in)

    // f(a * b) == f(a) * f(b)?
    def productMapLaw[A,B](p1:Parser[A],p2:Parser[A])(in:Gen[String])(f:A => B):Prop =
      equal(product(p1,p2).map{case (a,b) => (f(a),f(b))},product(p1.map(f),p2.map(f)))(in)
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
  def latest = stack.lastOption
  def latestLocation = latest.map(_._1)
  def label[A](s: String): ParseError = ParseError(latestLocation.map((_,s)).toList)
  def scope[A](s: String): ParseError = ParseError(latest.map{ p => (p._1, s"$s: ${p._2}")}.toList)
}
