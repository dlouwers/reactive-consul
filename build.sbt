import Dependencies._

import scalariform.formatter.preferences._

lazy val root = (project in file("."))
  .settings(
    name := "reactive-consul",
    organization := "com.xebia",
    version := "0.1.0",
    scalaVersion := "2.11.5"
  )
  .aggregate(client, dockerTestkit, example)

lazy val client = (project in file("client"))
  .settings(
    resolvers ++= Dependencies.resolutionRepos,
    libraryDependencies ++= Seq(
      sprayClient,
      sprayJson,
      akkaActor,
      retry,
      slf4j,
      akkaSlf4j,
      specs2 % "test,it",
      specs2mock,
      logback % "test,it",
      akkaTestKit
    ),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(RewriteArrowSymbols, true),
    scalaVersion := "2.11.5",
    scalacOptions in Test ++= Seq("-Yrangepos"),
    scalacOptions in IntegrationTest ++= Seq("-Yrangepos")
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( scalariformSettingsWithIt : _* )
  .dependsOn(dockerTestkit % "test,it"
  )

lazy val dockerTestkit = (project in file("docker-testkit"))
  .settings(
    resolvers ++= Dependencies.resolutionRepos,
    libraryDependencies ++= Seq(
      slf4j,
      specs2,
      specs2mock,
      spotifyDocker
    ),
    scalaVersion := "2.11.5"
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( scalariformSettingsWithIt : _* )


lazy val example = (project in file("example"))
  .aggregate(client)
  .dependsOn(client)
  .settings(libraryDependencies ++= Seq(akkaActor, sprayClient, sprayRouting, sprayJson))
  .settings(
    fork := true,
    libraryDependencies ++= Seq(akkaActor, sprayClient, sprayJson),
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )
  .enablePlugins(JavaAppPackaging)
  .settings(
    packageName in Docker := "reactive-consul-example",
    maintainer in Docker := "Dirk Louwers <dlouwers@xebia.com> & Marc Rooding <mrooding@xebia.com>",
    dockerExposedPorts in Docker := Seq(8080),
    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
  )

Revolver.settings.settings