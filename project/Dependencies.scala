import sbt._
import play._

object Dependencies {

  //versions
  val awsClientVersion = "1.9.30"
  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.44"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.5"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val membershipCommon = "com.gu" %% "membership-common" % "0.63"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.1.11"
  val googleAdminService = "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev53-1.20.0"
  val contentAPI = "com.gu" %% "content-api-client" % "5.2"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val s3 =  "com.amazonaws" % "aws-java-sdk-s3" % awsClientVersion
  val scalaTest =  "org.scalatest" %% "scalatest" % "2.2.4" % "test"

  //projects

  val frontendDependencies = Seq(identityCookie, googleAdminService, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, playFilters,sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, s3, scalaTest)

}
