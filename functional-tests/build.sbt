name := "membership-tests"

version := "1.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4",
  "org.seleniumhq.selenium" % "selenium-java" % "2.44.0",
  "com.gu" %% "scala-automation" % "1.44" % "test",
  "com.gu" %% "scala-automation-tstash-logger" % "1.8" % "test",
  "com.gu" %% "identity-test-users" % "0.4"
)

resolvers ++= Seq(
  "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"
)
