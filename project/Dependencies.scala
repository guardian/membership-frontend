import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.12.111"
  val jacksonVersion = "2.11.4"
  //libraries
  val sentryRavenLogback = "io.sentry" % "sentry-logback" % "1.7.30"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "2.2.2"
  val identityAuthPlay = "com.gu.identity" %% "identity-auth-play" % "3.251"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.8"
  val membershipCommon = "com.gu" %% "membership-common" % "0.608"
  val contentAPI = "com.gu" %% "content-api-client-default" % "17.22"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters
  val playCache = PlayImport.ehcache
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val scalaTest =  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.5"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "4.15.0" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val enumPlay = "com.beachape" %% "enumeratum-play" % "1.7.0"
  val catsCore = "org.typelevel" %% "cats-core" % "2.6.1"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
  val kinesisLogbackAppender = "com.gu" % "kinesis-logback-appender" % "1.4.4"
  val logstash = "net.logstash.logback" % "logstash-logback-encoder" % "6.6"
  val googleAuth = "com.gu.play-googleauth" %% "play-v28" % "2.1.1"
  // vvv below here. All the dependencies are to force upgrades to versions of the libs without vulnerabilities
  val libthrift = "org.apache.thrift" % "libthrift" % "0.15.0"
  // ^^^ above here

  val frontendDependencies =  Seq(googleAuth, scalaUri, membershipCommon, enumPlay,
    contentAPI, playWS, playFilters, playCache, sentryRavenLogback, awsSimpleEmail, scalaz, pegdown,
    PlayImport.specs2 % "test", specs2Extra, identityAuthPlay, identityTestUsers, catsCore, scalaLogging, kinesisLogbackAppender, logstash,
   libthrift, scalaTest)


}
