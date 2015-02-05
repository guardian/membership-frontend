import sbt._
import play._

object Dependencies {

  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.44"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.4"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.4"
  val membershipCommon = "com.gu" %% "membership-common" % "0.50"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.1.10"
  val contentAPI = "com.gu" %% "content-api-client" % "5.1"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val playFilters = PlayImport.filters
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % "1.9.16"
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"

  //projects

  val frontendDependencies = Seq(identityCookie, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, playFilters,sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt)

}
