import Dependencies._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.pgp.PgpKeys
import scalariform.formatter.preferences._
sonatypeProfileName := "com.crobox"

// Common variables
lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.12.11", "2.13.1"),
  organization := "com.crobox",
  resolvers ++= Dependencies.resolutionRepos,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
  )

lazy val reactiveConsul = (project in file("."))
  .settings( commonSettings: _* )
  .settings( publishSettings: _* )
  .aggregate(client, dnsHelper, dockerTestkit/*, example*/)


lazy val dnsHelper = (project in file("dns-helper"))
  .settings( commonSettings: _* )
  .settings( publishSettings: _* )
  .settings(
    name := "reactive-consul-dns",
    publishArtifact in Compile := true,
    publishArtifact in makePom := true,
    publishArtifact in Test := false,
    publishArtifact in IntegrationTest := false,
    fork := true,
    libraryDependencies ++= Seq(
      spotifyDns
    ),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(RewriteArrowSymbols, true)
  )

lazy val client = (project in file("client"))
  .settings( commonSettings: _* )
  .settings( publishSettings: _* )
  .settings(
    name := "reactive-consul",
    publishArtifact in Compile := true,
    publishArtifact in makePom := true,
    publishArtifact in Test := false,
    publishArtifact in IntegrationTest := false,
    fork := true,
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaHttpSprayJson,
      akkaActor,
      akkaStream,
      slf4j,
      akkaSlf4j,
      scalaTest % "it,test",
      scalaMock % "test",
      logback % "it,test",
      akkaTestKit % "it,test",
      spotifyDocker % "it,test"
    ),
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(RewriteArrowSymbols, true)
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( SbtScalariform.scalariformSettingsWithIt : _* )
  .dependsOn(dockerTestkit % "it-internal")

lazy val dockerTestkit = (project in file("docker-testkit"))
  .settings( commonSettings: _* )
  .settings(
    libraryDependencies ++= Seq(
      slf4j,
      scalaTest,
      spotifyDocker
    )
  )
  .configs( IntegrationTest )
  .settings( Defaults.itSettings : _* )
  .settings( SbtScalariform.scalariformSettingsWithIt : _* )
  .settings( publishSettings: _* )


//lazy val example = (project in file("example"))
//  .aggregate(client)
//  .dependsOn(client, dnsHelper)
//  .settings( commonSettings: _* )
//  .settings(
//    crossScalaVersions := Seq()
//  )
//  .settings(
//      libraryDependencies ++= Seq(
//        sprayClient,
//        sprayRouting,
//        sprayJson,
//        slf4j,
//        logback
//      )
//  )
//  .settings(
//    fork := true,
//    libraryDependencies ++= Seq(
//      akkaActor,
//      sprayClient,
//      sprayJson
//    )
//  )
//  .settings( publishSettings: _* )
//  .enablePlugins(JavaAppPackaging)
//  .settings(
//    packageName in Docker := "reactive-consul-example",
//    maintainer in Docker := "Dirk Louwers <dirk.louwers@stormlantern.nl>",
//    dockerExposedPorts in Docker := Seq(8080),
//    dockerExposedVolumes in Docker := Seq("/opt/docker/logs")
//  )

lazy val publishSettings = Seq(
  publishArtifact := false,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction := PgpKeys.publishSigned.value,
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
    )
)

Revolver.settings.settings
