

name := "Membership"

version := "1.0-SNAPSHOT"

organization := "com.gu"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  cache,
  "com.github.nscala-time" %% "nscala-time" % "1.0.0"
)

play.Project.playScalaSettings

playArtifactDistSettings

magentaPackageName := "membership-app"

parallelExecution in Global := false

scalariformSettings

ScoverageSbtPlugin.instrumentSettings

//Scala style settings
org.scalastyle.sbt.ScalastylePlugin.Settings

org.scalastyle.sbt.PluginKeys.failOnError := true

ScoverageSbtPlugin.ScoverageKeys.excludedPackages in ScoverageSbtPlugin.scoverage := "<empty>;Reverse.*;Routes"

lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}

(test in Test) <<= (test in Test) dependsOn testScalaStyle
