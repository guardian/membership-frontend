import play.sbt.PlayImport
import sbt._

object Dependencies {

  //versions
  val awsClientVersion = "1.12.111"
//  val jacksonVersion = "2.11.4"
  //libraries
  val sentryRavenLogback = "io.sentry" % "sentry-logback" % "1.7.5"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "2.2.2"
  val identityAuthPlay = "com.gu.identity" %% "identity-auth-play" % "3.248"
  val identityTestUsers = "com.gu" %% "identity-test-users" % "0.8"
  val membershipCommon = "com.gu" %% "membership-common" % "0.608"
  val contentAPI = "com.gu" %% "content-api-client-default" % "17.17"
  val playWS = PlayImport.ws
  val playFilters = PlayImport.filters
  val playCache = PlayImport.ehcache
//  val playIteratees = "com.typesafe.play" %% "play-iteratees" % "2.6.1"
  val awsSimpleEmail = "com.amazonaws" % "aws-java-sdk-ses" % awsClientVersion
  val scalaTest =  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.5"
  val specs2Extra = "org.specs2" %% "specs2-matcher-extra" % "4.5.1" % "test"
  val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
  val enumPlay = "com.beachape" %% "enumeratum-play" % "1.7.0"
  val catsCore = "org.typelevel" %% "cats-core" % "2.6.1"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
  val kinesisLogbackAppender = "com.gu" % "kinesis-logback-appender" % "1.4.0"
  val logstash = "net.logstash.logback" % "logstash-logback-encoder" % "4.9"
//  val dataFormat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
//  // This is required to force aws libraries to use the latest version of jackson
//  val jacksonDataBind =  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
//  val jacksonScalaModule = "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
//  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion
//  var jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
//  var jacksonDataType = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion
  val googleAuth = "com.gu.play-googleauth" %% "play-v28" % "2.1.1"
  // All the dependencies below here are to force upgrades to versions of the libs without vulnerabilities
  val bcprovJdk15on = "org.bouncycastle" % "bcprov-jdk15on" % "1.60"  //-- added explicitly - snyk report avoid logback vulnerability
  val libthrift = "org.apache.thrift" % "libthrift" % "0.14.1"
  val tomCat = "org.apache.tomcat.embed" % "tomcat-embed-core" % "8.5.63"
  val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.13"
  //projects

  val frontendDependencies =  Seq(googleAuth, scalaUri, membershipCommon, enumPlay,
    contentAPI, playWS, playFilters, playCache, /*playIteratees,*/ sentryRavenLogback, awsSimpleEmail, scalaz, pegdown,
    PlayImport.specs2 % "test", specs2Extra, identityAuthPlay, identityTestUsers, catsCore, scalaLogging, kinesisLogbackAppender, logstash, //dataFormat,
//    jacksonDataType, jacksonDataBind, jacksonAnnotations, jacksonCore,
    bcprovJdk15on, libthrift, tomCat, httpComponents, scalaTest)


}
