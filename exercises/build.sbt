name := """Functional Programming in Scala: Exercises"""

version := "1.0"

scalaVersion := "2.11.6"

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "org.clapper" %% "grizzled-scala" % "1.3"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"
// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.11"