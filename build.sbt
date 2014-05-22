

name := "Membership"

version := "1.0-SNAPSHOT"

organization := "com.gu"

scalaVersion := "2.10.4"

resolvers += "Guardian Github Releases" at "http://guardian.github.io/maven/repo-releases"

libraryDependencies ++= Seq(
  cache,
  "com.github.nscala-time" %% "nscala-time" % "1.0.0",
  "com.gu.identity" %% "identity-cookie" % "3.40",
  "com.gu.identity" %% "identity-model" % "3.40"
)

play.Project.playScalaSettings

playArtifactDistSettings

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
  // so this next one is constant to avoid it always recompiling on dev machines.
  // we only really care about build time on teamcity, when a constant based on when
  // it was loaded is just fine
  BuildInfoKey.constant("buildTime", System.currentTimeMillis)
)

buildInfoPackage := "app"

magentaPackageName := "app"

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
