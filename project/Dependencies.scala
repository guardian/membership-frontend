import sbt._
import play._

object Dependencies {

  val identity = "3.42"

  //libraries
  val identityCookie = "com.gu.identity" %% "identity-cookie" % identity
  val identityModel = "com.gu.identity" %% "identity-model" % identity
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.1"
  val membershipCommon = "com.gu" %% "membership-common" % "0.36"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters

  //projects
  val frontendDependencies = Seq(identityCookie, identityModel, scalaUri, membershipCommon, playWS, playFilters)
}
