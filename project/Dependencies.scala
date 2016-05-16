import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "spray repo" at "http://repo.spray.io",
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
  )

  val sprayVersion  = "1.3.3"
  val akkaVersion   = "2.4.4"

  val sprayClient   = "io.spray"                    %% "spray-client"                 % sprayVersion
  val sprayRouting  = "io.spray"                    %% "spray-routing"                % sprayVersion
  val sprayJson     = "io.spray"                    %% "spray-json"                   % "1.3.1"
  val akkaActor     = "com.typesafe.akka"           %% "akka-actor"                   % akkaVersion
  val akkaSlf4j     = "com.typesafe.akka"           %% "akka-slf4j"                   % akkaVersion
  val slf4j         = "org.slf4j"                   %  "slf4j-api"                    % "1.7.5"
  val logback       = "ch.qos.logback"              %  "logback-classic"              % "1.0.9"
  val retry         = "me.lessis"                   %% "retry"                        % "0.2.0"
  val spotifyDocker = "com.spotify"                 %  "docker-client"                % "3.5.12"
  val spotifyDns    = "com.spotify"                 %  "dns"                          % "3.0.1"
  val scalaTest     = "org.scalatest"               %  "scalatest_2.11"               % "2.2.4"
  val scalaMock     = "org.scalamock"               %% "scalamock-scalatest-support"  % "3.2.2"
  val akkaTestKit   = "com.typesafe.akka"           %% "akka-testkit"                 % akkaVersion   % "test,it"
}
