import Dependencies._
import sbt.Keys._
import ReleaseTransformations._

import scalariform.formatter.preferences._

lazy val root = (project in file("."))
  .settings(
    name := "reactive-consul",
    organization := "com.stormlantern",
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
    scalaVersion := "2.11.5"
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( scalariformSettingsWithIt : _* )
  .settings( publishSettings : _* )
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
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )
  .enablePlugins(JavaAppPackaging)
  .settings(
    packageName in Docker := "reactive-consul-example",
    maintainer in Docker := "Dirk Louwers <dlouwers@xebia.com> & Marc Rooding <mrooding@xebia.com>",
    dockerExposedPorts in Docker := Seq(8080),
    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
  )

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>http://github.com/dlouwers/reactive-consul</url>
      <licenses>
        <license>
          <name>MIT</name>
          <url>https://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:dlouwers/reactive-consul.git</url>
        <connection>scm:git@github.com:dlouwers/reactive-consul.git</connection>
      </scm>
      <developers>
        <developer>
          <id>dlouwers</id>
          <name>Dirk Louwers</name>
          <url>http://github.com/dlouwers</url>
        </developer>
      </developers>
    ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

Revolver.settings.settings
