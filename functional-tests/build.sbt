name := "membership-tests"

version := "1.1"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "2.0",
  "org.seleniumhq.selenium" % "selenium-java" % "2.42.2",
  "com.gu" % "web-automation-core-scala_2.10" % "1.9",
  "com.google.code.findbugs" % "jsr305" % "1.3.+"
)

resolvers ++= Seq(
  "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging"
)
