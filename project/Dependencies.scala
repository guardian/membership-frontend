import sbt._
import play.sbt.PlayImport

object Dependencies {

  //versions
  val awsClientVersion = "1.9.30"
  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.44"
  val identityPlayAuth = "com.gu.identity" %% "identity-play-auth" % "0.4"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.5"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val membershipCommon = "com.gu" %% "membership-common" % "0.88-SNAPSHOT"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.3.0"
  val contentAPI = "com.gu" %% "content-api-client" % "6.4"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val s3 =  "com.amazonaws" % "aws-java-sdk-s3" % awsClientVersion
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test"

  //projects

  val frontendDependencies = Seq(identityCookie, identityPlayAuth, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, playFilters,sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, s3,
    PlayImport.specs2 % "test",
    scalaTest)

}
