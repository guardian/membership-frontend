import sbt._
import sbt.Keys._

import play._
import PlayArtifact._
import sbtbuildinfo.Plugin._

trait Membership {

  val appVersion = "1.0-SNAPSHOT"

  def buildInfoPlugin = buildInfoSettings ++ Seq(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
      BuildInfoKey.constant("buildTime", System.currentTimeMillis)
    ),
    buildInfoPackage := "app"
  )

  def coveragePlugin = ScoverageSbtPlugin.instrumentSettings ++ Seq(
    ScoverageSbtPlugin.ScoverageKeys.excludedPackages in ScoverageSbtPlugin.scoverage := "<empty>;Reverse.*;Routes"
  )

  def commonSettings = Seq(
    organization := "com.gu",
    version := appVersion,
    scalaVersion := "2.10.4",
    resolvers += "Guardian Github Releases" at "http://guardian.github.io/maven/repo-releases",
    parallelExecution in Global := false,
    javaOptions in Test += "-Dconfig.resource=dev.conf"
  ) ++ buildInfoPlugin ++ coveragePlugin

  def lib(name: String) = Project(name, file(name)).enablePlugins(PlayScala).settings(commonSettings: _*)

  def app(name: String) = lib(name).settings(playArtifactDistSettings: _*).settings(magentaPackageName := name)
}

object Membership extends Build with Membership {
  val scalaforce = lib("scalaforce")
    .settings(libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-agent" % "2.2.0",
        PlayImport.ws
      )
    )

  val frontend = app("frontend").dependsOn(scalaforce)
    .settings(addCommandAlias("devrun", "run -Dconfig.resource=dev.conf 9100"): _*)
    .settings(
      libraryDependencies ++= Seq(
        "com.github.nscala-time" %% "nscala-time" % "1.0.0",
        "com.typesafe.akka" %% "akka-agent" % "2.2.0",
        "com.gu.identity" %% "identity-cookie" % "3.40",
        "com.gu.identity" %% "identity-model" % "3.40",
        "com.github.seratch" %% "awscala" % "0.2.1",
        "com.netaporter" %% "scala-uri" % "0.4.1",
        PlayImport.ws
      )
    )

  val root = Project("root", base=file(".")).aggregate(scalaforce, frontend)
}

