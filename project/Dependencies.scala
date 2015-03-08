import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io"
  )

  val sprayVersion = "1.3.2"
  val akkaVersion = "2.3.7"

  val sprayClient = "io.spray"                    %%  "spray-client"  % sprayVersion
  val sprayJson   = "io.spray"                    %%  "spray-json"    % "1.3.1"
  val akkaActor   = "com.typesafe.akka"           %%  "akka-actor"    % akkaVersion
  val akkaTestKit = "com.typesafe.akka"           %%  "akka-testkit"  % akkaVersion
  val specs2      = "org.specs2"                  %%  "specs2-core"   % "2.4.13"
}