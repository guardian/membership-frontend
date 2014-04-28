name := "Membership"

version := "1.0-SNAPSHOT"

organization := "com.gu"

scalaVersion := "2.11"


libraryDependencies ++= Seq(
  cache,
  "com.stripe" % "stripe-java" % "1.12.0"
)

play.Project.playScalaSettings
