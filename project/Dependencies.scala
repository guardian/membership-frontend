import sbt._
import play._

object Dependencies {

  //libraries
  val sentryRavenLogback = "net.kencochrane.raven" % "raven-logback" % "6.0.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % "3.44"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.4"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.4"
  val membershipCommon = "com.gu" %% "membership-common" % "0.45"
  val playGoogleAuth = "com.gu" %% "play-googleauth" % "0.1.10"
  val contentAPI = "com.gu" %% "content-api-client" % "3.5"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters

  //projects

  val frontendDependencies = Seq(identityCookie, playGoogleAuth, identityTestUsers, scalaUri, membershipCommon,
    contentAPI, playWS, playFilters,sentryRavenLogback)

}
