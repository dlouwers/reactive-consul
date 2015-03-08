import Dependencies._

lazy val root = (project in file(".")).
  settings(
    name := "reactive-consul",
    organization := "nl.stormlantern",
    version := "0.1.0",
    scalaVersion := "2.11.5"
  )

lazy val client = (project in file("client"))

lazy val tools = (project in file("tools"))
  .aggregate(client)
  .dependsOn(client)

lazy val example = (project in file("example"))
  .aggregate(client, tools)
  .dependsOn(client, tools)
  .settings(libraryDependencies ++= Seq(akkaActor, sprayClient, sprayJson))

Revolver.settings