name := "Membership"

version := "1.0-SNAPSHOT"

organization := "com.gu"

scalaVersion := "2.10.4"


libraryDependencies ++= Seq(
  cache,
  "com.github.nscala-time" %% "nscala-time" % "1.0.0",
  "com.stripe" % "stripe-java" % "1.12.0"
)

play.Project.playScalaSettings
