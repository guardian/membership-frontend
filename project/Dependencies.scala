import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.10.50"
  //libraries
  val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "7.2.3"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.6"
  val memsubCommonPlayAuth = "com.gu" %% "memsub-common-play-auth" % "0.7"
  val membershipCommon = "com.gu" %% "membership-common" % "0.209"
  val contentAPI = "com.gu" %% "content-api-client" % "8.5"
  val playWS = PlayImport.ws
  val playCache = PlayImport.cache
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0-M4" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.1"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "2.52.0" % "test"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "3.6" % "test"

  //projects

  val frontendDependencies = Seq(memsubCommonPlayAuth, scalaUri, membershipCommon,
    contentAPI, playWS, playCache, sentryRavenLogback, awsSimpleEmail, snowPlow, bCrypt, scalaz,
    PlayImport.specs2 % "test", specs2Extra)

  val acceptanceTestDependencies = Seq(scalaTest, selenium, memsubCommonPlayAuth)

}
