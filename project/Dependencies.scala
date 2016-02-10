import sbt._
import play.sbt.PlayImport

object Dependencies {

  //versions
  val awsClientVersion = "1.10.50"
  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityPlayAuth = "com.gu.identity" %% "identity-play-auth" % "0.13"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.5"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val membershipCommon = "com.gu" %% "membership-common" % "0.156"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.3.3"
  val contentAPI = "com.gu" %% "content-api-client" % "6.4"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val s3 =  "com.amazonaws" % "aws-java-sdk-s3" % awsClientVersion
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "2.48.2" % "test"

  //projects

  val frontendDependencies = Seq(identityPlayAuth, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, playFilters,sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, s3, scalaz,
    PlayImport.specs2 % "test")

  val acceptanceTestDependencies = Seq(scalaTest, selenium, identityTestUsers)

}
