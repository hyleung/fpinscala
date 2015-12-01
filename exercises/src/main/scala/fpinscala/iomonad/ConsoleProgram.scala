package fpinscala.iomonad

import fpinscala.iomonad.IO3.Console
import fpinscala.iomonad.IO3.Console._
import fpinscala.iomonad.IO3._

/**
  * Created by hyleung on 2015-11-30.
  */
object ConsoleProgram {

  def main (args: Array[String]) {
    val f1: Free[Console, Option[String]] = for {
      _ <- printLn("I can only interact with the console.")
      ln <- readLn
    } yield ln
    runConsoleFunction0(f1)() match {
      case Some(s) => println(s"Got the message! $s")
      case None => println("Y U NO MESSAGE?!")
    }

  }
}
