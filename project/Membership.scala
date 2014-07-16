import sbt._
import sbt.Keys._

import play._
import PlayArtifact._
import sbtbuildinfo.Plugin._
import Dependencies._

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

  val commonSettings = Seq(
    organization := "com.gu",
    version := appVersion,
    scalaVersion := "2.10.4",
    resolvers += "Guardian Github Releases" at "http://guardian.github.io/maven/repo-releases",
    parallelExecution in Global := false,
    javaOptions in Test += "-Dconfig.resource=dev.conf"
  ) ++ buildInfoPlugin ++ playArtifactDistSettings ++ coveragePlugin

  def app(name: String) = Project(name, file(name)).enablePlugins(PlayScala)
    .settings(commonSettings: _*)
    .settings(magentaPackageName := name)
}

object Membership extends Build with Membership {
  val frontend = app("frontend")
                .settings(libraryDependencies ++= frontendDependencies: _*)
                .settings(addCommandAlias("devrun", "run -Dconfig.resource=dev.conf 9100"): _*)

  val root = Project("root", base=file(".")).aggregate(frontend)
}