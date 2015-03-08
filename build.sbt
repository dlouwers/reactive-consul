lazy val root = (project in file(".")).
  settings(
    name := "reactive-consul",
    organization := "nl.stormlantern",
    version := "0.1.0",
    scalaVersion := "2.11.4"
  )

lazy val client = (project in file("client"))

lazy val tools = (project in file("tools"))
  .aggregate(client)
  .dependsOn(client)

lazy val example = (project in file("example"))
  .aggregate(client, tools)
  .dependsOn(client, tools)

libraryDependencies ++= {
  val akkaV = "2.3.7"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"                    %%  "spray-client"  % sprayV,
    "io.spray"                    %%  "spray-json"    % sprayV,
    "com.typesafe.akka"           %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"           %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"                  %%  "specs2-core"   % "2.4.13" % "test"
  )
}

Revolver.settings