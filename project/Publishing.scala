import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import SonatypeKeys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.pgp._

object Publishing {
  lazy val publishingSettings = sonatypeSettings ++ Seq(
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"

      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- sys.env.get("SONATYPE_USERNAME")
        password <- sys.env.get("SONATYPE_PASSWORD")
      } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
    pomExtra := (
      <url>https://github.com/dlouwers/reactive-consul#README.md</url>
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
      ))
}
