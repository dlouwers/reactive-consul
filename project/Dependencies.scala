import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
  )

  val sprayVersion  = "1.3.2"
  val akkaVersion   = "2.3.7"
  val specsVersion  = "3.0.1"

  val sprayClient   = "io.spray"                    %%  "spray-client"    % sprayVersion
  val sprayRouting  = "io.spray"                    %%  "spray-routing"   % sprayVersion
  val sprayJson     = "io.spray"                    %%  "spray-json"      % "1.3.1"
  val akkaActor     = "com.typesafe.akka"           %%  "akka-actor"      % akkaVersion
  val slf4j         = "org.slf4j"                   %   "slf4j-api"       % "1.7.5"
  val logback       = "ch.qos.logback"              %   "logback-classic" % "1.0.9"
  val retry         = "me.lessis"                   %%  "retry"           % "0.2.0"
  val akkaTestKit   = "com.typesafe.akka"           %%  "akka-testkit"    % akkaVersion   % "test,it"
  val specs2        = "org.specs2"                  %%  "specs2-core"     % specsVersion  % "test,it"
  val spotifyDocker = "com.spotify"                 %   "docker-client"   % "2.7.14"      % "test,it"
}