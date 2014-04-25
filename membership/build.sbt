name := "membership"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
  "com.stripe" % "stripe-java" % "1.12.0"
)

play.Project.playScalaSettings
