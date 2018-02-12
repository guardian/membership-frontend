import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.11.226"
  //libraries
  val sentryRavenLogback = "com.getsentry.raven" % "raven-logback" % "8.0.3"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.16"
  val identityPlayAuth = "com.gu.identity" %% "identity-play-auth" % "2.1"
  val membershipCommon = "com.gu" %% "membership-common" % "0.497"
  val contentAPI = "com.gu" %% "content-api-client" % "11.40"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters
  val playCache = PlayImport.ehcache
  val playIteratees = "com.typesafe.play" %% "play-iteratees" % "2.6.1"
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val sqs = "com.amazonaws" % "aws-java-sdk-sqs" % awsClientVersion
  val snowPlow = "com.snowplowanalytics" % "snowplow-java-tracker" % "0.5.2-SNAPSHOT"
  val bCrypt = "com.github.t3hnar" %% "scala-bcrypt" % "3.1"
  val scalaTest =  "org.scalatestplus" %% "play" % "1.4.0" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.7"
  val selenium = "org.seleniumhq.selenium" % "selenium-java" % "3.5.3" % "test"
  val seleniumHtmlUnitDriver = "org.seleniumhq.selenium" % "htmlunit-driver" % "2.29.0" % "test"
  val seleniumManager = "io.github.bonigarcia" % "webdrivermanager" % "2.1.0" % "test"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "3.6.6" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val enumPlay = "com.beachape" %% "enumeratum-play" % "1.3.7"
  val catsCore = "org.typelevel" %% "cats-core" % "1.0.1"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  val kinesisLogbackAppender = "com.gu" % "kinesis-logback-appender" % "1.4.0"
  val logstash = "net.logstash.logback" % "logstash-logback-encoder" % "4.9"
  val dataFormat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.8.10"
  val acquisitionEventProducer = "com.gu" %% "acquisition-event-producer-play26" % "2.0.5-SNAPSHOT" % "compile" excludeAll(
    ExclusionRule(organization = "org.scalatest"),
    ExclusionRule(organization = "org.scalactic")
  )
  val googleAuth = "com.gu" %% "play-googleauth" % "0.7.2"
  //projects

  val frontendDependencies =  Seq(googleAuth, scalaUri, membershipCommon, enumPlay,
    contentAPI, playWS, playFilters, playCache, playIteratees, sentryRavenLogback, awsSimpleEmail, sqs, snowPlow, bCrypt, scalaz, pegdown,
    PlayImport.specs2 % "test", specs2Extra, identityPlayAuth, catsCore, scalaLogging, kinesisLogbackAppender, logstash, dataFormat,
    acquisitionEventProducer)

  val acceptanceTestDependencies = Seq(scalaTest, selenium, seleniumHtmlUnitDriver, seleniumManager)

}
