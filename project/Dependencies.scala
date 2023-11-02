import sbt._

object Dependencies {

  val PekkoVersion = "1.0.0"
  val PekkoHttpVersion = "1.0.0"

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.15", "org.scalamock" %% "scalamock" % "5.2.0")
}
