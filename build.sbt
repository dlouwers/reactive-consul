import Dependencies._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val root = (project in file("."))
  .settings(
    name := "reactive-consul",
    organization := "stormlantern",
    version := "0.1.0",
    scalaVersion := "2.11.8"
  )
  .aggregate(client, dockerTestkit, example)

lazy val client = (project in file("client"))
  .settings(
    fork := true,
    resolvers ++= Dependencies.resolutionRepos,
    libraryDependencies ++= Seq(
      sprayClient,
      sprayJson,
      akkaActor,
      retry,
      spotifyDns,
      slf4j,
      akkaSlf4j,
      scalaTest % "test, it",
      scalaMock % "test",
      logback % "test,it",
      akkaTestKit
    ),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(RewriteArrowSymbols, true),
    scalaVersion := "2.11.8"
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( SbtScalariform.scalariformSettingsWithIt : _* )
  .dependsOn(dockerTestkit % "test,it")

lazy val dockerTestkit = (project in file("docker-testkit"))
  .settings(
    resolvers ++= Dependencies.resolutionRepos,
    libraryDependencies ++= Seq(
      slf4j,
      scalaTest,
      spotifyDocker
    ),
    scalaVersion := "2.11.8"
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( SbtScalariform.scalariformSettingsWithIt : _* )


lazy val example = (project in file("example"))
  .aggregate(client)
  .dependsOn(client)
  .settings(
      libraryDependencies ++= Seq(
        sprayClient,
        sprayRouting,
        sprayJson,
        slf4j,
        logback
      )
  )
  .settings(
    fork := true,
    libraryDependencies ++= Seq(akkaActor, sprayClient, sprayJson),
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )
  .enablePlugins(JavaAppPackaging)
  .settings(
    packageName in Docker := "reactive-consul-example",
    maintainer in Docker := "Dirk Louwers <dirk.louwers@stormlantern.nl>",
    dockerExposedPorts in Docker := Seq(8080),
    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
  )

Revolver.settings.settings
