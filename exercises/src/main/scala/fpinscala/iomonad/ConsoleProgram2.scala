package fpinscala.iomonad

import fpinscala.iomonad.IO3.Console
import fpinscala.iomonad.IO3.Console._
import fpinscala.iomonad.IO3._

/**
 *   * Created by hyleung on 2015-11-30.
 *     */
object ConsoleProgram2 {
  import fpinscala.parallelism.Nonblocking.Par

  def main (args: Array[String]) {
    val f1: Free[Console, Option[String]] = for {
      _ <- printLn("I can only interact with the console.")
      ln <- readLn
    } yield ln
    runConsoleFunction0(f1)() match {
      case Some(s) => println(s"Got the message using Translate[F,G]! $s")
      case None => println("Y U NO MESSAGE?!")
    }

  }
  def runFree[F[_],G[_],A](free:Free[F,A])(t:Translate[F,G])(implicit G: Monad[G]):G[A] =
    step(free) match {
      case Return(a) => G.unit(a)
      case Suspend(r) => t(r)
      case FlatMap(Suspend(r), f) => G.flatMap(t(r))(a => runFree(f(a))(t))
      case _ => sys.error("Impossible, since `step` eliminates these cases")
    }
  val consoleToFunction0 =
    new Translate[Console,Function0]{ def apply[A](a: Console[A]) = a.toThunk }
  val consoleToPar =
    new Translate[Console,Par]{ def apply[A](a: Console[A]) = a.toPar }

  def runConsoleFunction0[A](a: Free[Console,A]): () => A =
    runFree[Console,Function0,A](a)(consoleToFunction0)
  def runConsolePar[A](a: Free[Console,A]): Par[A] =
    runFree[Console,Par,A](a)(consoleToPar)
}

