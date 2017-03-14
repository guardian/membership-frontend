import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.11.95"
  //libraries
  val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "7.2.3"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.16"
  val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "0.9" // v0.9 is the latest version published for Play 2.4...
  val membershipCommon = "com.gu" %% "membership-common" % "0.382-SNAPSHOT"
  val contentAPI = "com.gu" %% "content-api-client" % "8.5"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters
  val playCache = PlayImport.cache
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "3.0.1" % "test"
  val seleniumHtmlUnitDriver ="org.seleniumhq.selenium" % "htmlunit-driver" % "2.23" % "test"
  val seleniumManager = "io.github.bonigarcia" % "webdrivermanager" % "1.4.10" % "test"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "3.6" % "test"
  val dispatch = "net.databinder.dispatch" %% "dispatch-core" % "0.11.3"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"

  //projects

  val frontendDependencies =  Seq(memsubCommonPlayAuth, scalaUri, membershipCommon,
    contentAPI, playWS, playFilters, playCache, sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, scalaz, pegdown,
    PlayImport.specs2 % "test", specs2Extra, dispatch)

  val acceptanceTestDependencies = Seq(memsubCommonPlayAuth, scalaTest, selenium, seleniumHtmlUnitDriver, seleniumManager)

}
