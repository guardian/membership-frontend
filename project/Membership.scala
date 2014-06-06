import sbt._
import sbt.Keys._

import PlayArtifact._
import sbtbuildinfo.Plugin._

trait Membership {
  val version = "1.0-SNAPSHOT"

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

  def scalaStylePlugin = {
    lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

    org.scalastyle.sbt.ScalastylePlugin.Settings ++ Seq(
      org.scalastyle.sbt.PluginKeys.failOnError := true,
      testScalaStyle := {
        org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
      },
      (test in Test) <<= (test in Test) dependsOn testScalaStyle
    )
  }

  val commonDependencies = Seq(
    "com.github.nscala-time" %% "nscala-time" % "1.0.0",
    "com.typesafe.akka" %% "akka-agent" % "2.2.0",
    "com.gu.identity" %% "identity-cookie" % "3.40",
    "com.gu.identity" %% "identity-model" % "3.40",
    "com.github.seratch" %% "awscala" % "0.2.1"
  )

  def commonSettings = Seq(
    organization := "com.gu",
    scalaVersion := "2.10.4",
    resolvers += "Guardian Github Releases" at "http://guardian.github.io/maven/repo-releases",
    libraryDependencies ++= commonDependencies,
    parallelExecution in Global := false,
    javaOptions in Test += "-Dconfig.resource=dev.conf"
  ) ++ buildInfoPlugin ++ playArtifactDistSettings ++ scalaStylePlugin ++ coveragePlugin

  def app(name: String) = play.Project(name, version, path=file(name)).settings(commonSettings: _*).settings(magentaPackageName := name)
}

object Membership extends Build with Membership {
  val frontend = app("frontend")

  val root = Project("root", base=file(".")).aggregate(frontend)
}

