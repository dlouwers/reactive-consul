import Dependencies._

import scalariform.formatter.preferences._

val groupName = "com.stormlantern"
val artifactBasename = "reactive-consul"
val artifactVersion = "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := artifactBasename,
    organization := groupName,
    version := artifactVersion,
    publishArtifact := false,
    scalaVersion := "2.11.5"
  )
  .aggregate(client, dockerTestkit, example)

lazy val client = (project in file("client"))
  .settings(
    name := artifactBasename + "-client",
    organization := groupName,
    version := artifactVersion,
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
      scalaMock,
      logback % "test,it",
      akkaTestKit
    ),
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/dlouwers/reactive-consul</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>https://opensource.org/licenses/MIT</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:dlouwers/reactive-consul.git</url>
          <connection>scm:git:git@github.com:dlouwers/reactive-consul.git</connection>
        </scm>),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(RewriteArrowSymbols, true),
    scalaVersion := "2.11.5"
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( scalariformSettingsWithIt : _* )
  .dependsOn(dockerTestkit % "test,it")

lazy val dockerTestkit = (project in file("docker-testkit"))
  .settings(
    name := artifactBasename + "-docker-testkit",
    organization := groupName,
    version := artifactVersion,
    resolvers ++= Dependencies.resolutionRepos,
    libraryDependencies ++= Seq(
      slf4j,
      scalaTest,
      spotifyDocker
    ),
    scalaVersion := "2.11.5",
    publishArtifact := false
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( scalariformSettingsWithIt : _* )


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
    scalaVersion := "2.11.5",
    publishArtifact := false,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := artifactBasename + "-example",
    organization := groupName,
    version := artifactVersion,
    maintainer in Docker := "Dirk Louwers <dlouwers@xebia.com> & Marc Rooding <mrooding@xebia.com>",
    dockerExposedPorts in Docker := Seq(8080),
    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
  )

Revolver.settings.settings
