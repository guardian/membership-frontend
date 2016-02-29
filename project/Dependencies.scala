import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.10.50"
  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val membershipCommon = "com.gu" %% "membership-common" % "0.161"
  val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "0.4"
  val contentAPI = "com.gu" %% "content-api-client" % "6.4"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test"

  //projects

  val frontendDependencies = Seq(memsubCommonPlayAuth, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, scalaz,
    PlayImport.specs2 % "test")

  val acceptanceTestDependencies = Seq(scalaTest, selenium, memsubCommonPlayAuth)

}
