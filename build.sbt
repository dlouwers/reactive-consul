// Scala Formatting
ThisBuild / scalafmtVersion := "1.5.1"
ThisBuild / scalafmtOnCompile := false     // all projects
ThisBuild / scalafmtTestOnCompile := false // all projects

releaseCrossBuild := true

sonatypeProfileName := "com.crobox"

lazy val root = (project in file("."))
  .settings(
    publish := {},
    publishArtifact := false,
    inThisBuild(
      List(
        organization := "com.crobox.reactive-consul",
        scalaVersion := "2.13.8",
        crossScalaVersions := List("2.13.8"),
        javacOptions ++= Seq("-g", "-Xlint:unchecked", "-Xlint:deprecation", "-source", "11", "-target", "11"),
        scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:_", "-encoding", "UTF-8"),
        publishTo := {
          val nexus = "https://oss.sonatype.org/"
          if (version.value.trim.endsWith("SNAPSHOT"))
            Some("snapshots" at nexus + "content/repositories/snapshots")
          else
            Some("releases" at nexus + "service/local/staging/deploy/maven2")
        },
        pomExtra := {
          <url>https://github.com/crobox/reactive-consul</url>
            <licenses>
              <license>
                <name>MIT</name>
                <url>https://opensource.org/licenses/MIT</url>
                <distribution>repo</distribution>
              </license>
            </licenses>
            <scm>
              <url>git@github.com:crobox/reactive-consul.git</url>
              <connection>scm:git@github.com:crobox/reactive-consul.git</connection>
            </scm>
            <developers>
              <developer>
                <id>crobox</id>
                <name>crobox</name>
                <url>https://github.com/crobox</url>
              </developer>
              <developer>
                <id>dlouwers</id>
                <name>Dirk Louwers</name>
                <url>http://github.com/dlouwers</url>
              </developer>
            </developers>
        }
      )
    ),
    name := "reactive-consul"
  )
  .aggregate(client)

lazy val client: Project = (project in file("client"))
  .configs(Config.CustomIntegrationTest)
  .settings(Config.testSettings: _*)
  .settings(
    name := "client",
    sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    libraryDependencies ++= Seq(
      "ch.qos.logback"   % "logback-classic" % "1.4.7",
      "io.spray"         %% "spray-json"     % "1.3.6",
      "org.apache.pekko" %% "pekko-actor"    % Dependencies.PekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j"    % Dependencies.PekkoVersion,
      "org.apache.pekko" %% "pekko-stream"   % Dependencies.PekkoVersion,
      "org.apache.pekko" %% "pekko-http"     % Dependencies.PekkoHttpVersion,
      // test dependencies
      "org.apache.pekko" %% "pekko-testkit" % Dependencies.PekkoVersion % Test,
      "org.scalatest"    %% "scalatest"     % "3.2.15"                  % Test,
      "org.scalamock"    %% "scalamock"     % "5.2.0"                   % Test
    )
  )
