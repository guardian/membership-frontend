import sbt._
import play._

object Dependencies {

  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.44"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.5"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val membershipCommon = "com.gu" %% "membership-common" % "0.63"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.1.11"
  val googleApiClient = "com.google.api-client" % "google-api-client" % "1.19.1"
  val googleAdminService = "com.google.apis" % "google-api-services-admin" % "directory_v1-rev32-1.16.0-rc"
  val contentAPI = "com.gu" %% "content-api-client" % "5.2"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % "1.9.24"
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val s3 =  "com.amazonaws" % "aws-java-sdk-s3" % "1.9.30"

  //projects

  val frontendDependencies = Seq(identityCookie, googleApiClient, googleAdminService, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, playFilters,sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, s3)

}
