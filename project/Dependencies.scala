import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.11.478"
  val jacksonVersion = "2.9.8"
  //libraries
  val sentryRavenLogback = "io.sentry" % "sentry-logback" % "1.7.5"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.16"
  val identityPlayAuth = "com.gu.identity" %% "identity-play-auth" % "2.5"
  val membershipCommon = "com.gu" %% "membership-common" % "0.527"
  val contentAPI = "com.gu" %% "content-api-client" % "14.1"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters
  val playCache = PlayImport.ehcache
  val playIteratees = "com.typesafe.play" %% "play-iteratees" % "2.6.1"
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val scalaTest =  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.7"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "3.14.0" % "test"
  val seleniumManager = "io.github.bonigarcia" % "webdrivermanager" % "2.2.5" % "test"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "3.8.6" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val enumPlay = "com.beachape" %% "enumeratum-play" % "1.5.14"
  val catsCore = "org.typelevel" %% "cats-core" % "1.0.1"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  val kinesisLogbackAppender = "com.gu" % "kinesis-logback-appender" % "1.4.0"
  val logstash = "net.logstash.logback" % "logstash-logback-encoder" % "4.9"
  val dataFormat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
  val bcprovJdk15on = "org.bouncycastle" % "bcprov-jdk15on" % "1.60"  //-- added explicitly - snyk report avoid logback vulnerability
  // This is required to force aws libraries to use the latest version of jackson
  val jacksonDataBind =  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion
  var jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
  var jacksonDataType = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion
  val acquisitionEventProducer = "com.gu" %% "acquisition-event-producer-play26" % "4.0.0" % "compile" excludeAll(
    ExclusionRule(organization = "org.scalatest"),
    ExclusionRule(organization = "org.scalactic")
  )
  val googleAuth = "com.gu" %% "play-googleauth" % "0.7.6"
  //projects

  val frontendDependencies =  Seq(googleAuth, scalaUri, membershipCommon, enumPlay,
    contentAPI, playWS, playFilters, playCache, playIteratees, sentryRavenLogback, awsSimpleEmail, scalaz, pegdown,
    PlayImport.specs2 % "test", specs2Extra, identityPlayAuth, catsCore, scalaLogging, kinesisLogbackAppender, logstash, dataFormat,
    jacksonDataType, jacksonDataBind, jacksonAnnotations, jacksonCore,
    acquisitionEventProducer, bcprovJdk15on)

  val acceptanceTestDependencies = Seq(scalaTest, selenium, seleniumManager)

}
