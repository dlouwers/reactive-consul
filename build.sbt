lazy val root = (project in file(".")).
  settings(
    name := "reactive-consul",
    organization := "nl.stormlantern",
    version := "0.1.0",
    scalaVersion := "2.11.4"
  )

lazy val client = (project in file("client"))

lazy val tools = (project in file("tools"))

lazy val example = (project in file("example"))
