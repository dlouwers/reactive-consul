import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
  )

  val sprayVersion  = "1.3.4"
  val akkaVersion   = "2.4.14"

  val sprayClient   = "io.spray"                    %% "spray-client"                 % sprayVersion
  val akkaHttp      = "com.typesafe.akka"           %% "akka-http-core"               % "10.0.3"
  val sprayRouting  = "io.spray"                    %% "spray-routing"                % sprayVersion
  val sprayJson     = "io.spray"                    %% "spray-json"                   % "1.3.2"
  val akkaActor     = "com.typesafe.akka"           %% "akka-actor"                   % akkaVersion
  val akkaSlf4j     = "com.typesafe.akka"           %% "akka-slf4j"                   % akkaVersion
  val slf4j         = "org.slf4j"                   %  "slf4j-api"                    % "1.7.21"
  val logback       = "ch.qos.logback"              %  "logback-classic"              % "1.1.7"
  val spotifyDocker = "com.spotify"                 %  "docker-client"                % "3.6.8"
  val spotifyDns    = "com.spotify"                 %  "dns"                          % "3.1.4"
  val scalaTest     = "org.scalatest"               %% "scalatest"                    % "3.0.1"
  val scalaMock     = "org.scalamock"               %% "scalamock-scalatest-support"  % "3.6.0"
  val akkaTestKit   = "com.typesafe.akka"           %% "akka-testkit"                 % akkaVersion
}
