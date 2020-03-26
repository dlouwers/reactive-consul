import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "spray repo" at "https://repo.spray.io",
    "softprops-maven" at "https://dl.bintray.com/content/softprops/maven"
  )

  val akkaVersion     = "2.5.30"
  val akkaHttpVersion = "10.1.11"

  val akkaHttp          = "com.typesafe.akka" %% "akka-http-core"              % akkaHttpVersion
  val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json"        % akkaHttpVersion
  val akkaActor         = "com.typesafe.akka" %% "akka-actor"                  % akkaVersion
  val akkaStream        = "com.typesafe.akka" %% "akka-stream"                 % akkaVersion
  val akkaSlf4j         = "com.typesafe.akka" %% "akka-slf4j"                  % akkaVersion
  val slf4j             = "org.slf4j"         % "slf4j-api"                    % "1.7.30"
  val logback           = "ch.qos.logback"    % "logback-classic"              % "1.1.7"
  val spotifyDocker     = "com.spotify"       % "docker-client"                % "3.6.8"
  val spotifyDns        = "com.spotify"       % "dns"                          % "3.2.2"
  val scalaTest         = "org.scalatest"     %% "scalatest"                   % "3.1.1"
  val scalaMock         = "org.scalamock"     %% "scalamock"                   % "4.4.0"
  val akkaTestKit       = "com.typesafe.akka" %% "akka-testkit"                % akkaVersion
}
