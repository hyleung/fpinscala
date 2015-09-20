package fpinscala.parsing

import java.util.regex._
import scala.util.matching.Regex
import fpinscala.testing._
import fpinscala.testing.Prop._

trait Parsers[Parser[+_]] { self => // so inner classes may call methods of trait
  //primitives
  implicit def string(s:String):Parser[String]
  implicit def regex(r:Regex):Parser[String]
  def slice[A](p:Parser[A]):Parser[String] = ???
  def flatMap[A,B](p: Parser[A])(f: A => Parser[B]):Parser[B]
  def or[A](s1: Parser[A], s2: Parser[A]):Parser[A]
  def succeed[A](a:A):Parser[A]

  def run[A](p: Parser[A])(input: String):Either[ParseError,A]

  //combinators
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
    c <- f(a,b)
  } yield c

  val numA:Parser[Int] = char('a').many.map(_.size)

  def nChars(c:Char):Parser[Int] = "[0-9]+".r.flatMap{ s =>
      try {
        val n = s.toInt
        listOfN(n,char(c)).map{l => l.size}
      } catch {
        case e:NumberFormatException => ???
      }

  }

  def nChars2(c:Char):Parser[Int] = for {
    d <- "[0-9]+".r
    n = d.toInt //<-- didn't know you could do this!
    l <- listOfN(n,char(c))
  } yield l.size

  implicit def operators[A](p:Parser[A]): ParserOps[A] = ParserOps[A](p)
  implicit def asStringParser[A](a:A)(implicit f: A => Parser[String]):ParserOps[String] = ParserOps(f(a))

  case class ParserOps[A](p: Parser[A]) {
    def | [B>:A](p2:Parser[B]) = self.or(p,p2)
    def or [B>:A](p2:Parser[B]) = self.or(p,p2)
    def many [B>:A]:Parser[List[B]] = self.many(p)
    def map [B](f: A => B):Parser[B] = self.map(p)(f)
    def ** [B>:A](p2:Parser[B]):Parser[(A,B)] = self.product(p,p2)
    def flatMap [B](f: A => Parser[B]):Parser[B] = self.flatMap(p)(f)
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
}
