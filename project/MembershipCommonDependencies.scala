import sbt._

object MembershipCommonDependencies {

  val playJsonVersion = "2.9.3"
  val specs2Version = "4.19.0"

  //versions
  val awsClientVersion = "1.12.387"
  val dynamoDbVersion = "1.12.387"
  //libraries
  val supportInternationalisation = "com.gu" %% "support-internationalisation" % "0.16"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "2.2.0"
  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.32.0"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.6.14"
  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion
  val playJsonJoda = "com.typesafe.play" %% "play-json-joda" % playJsonVersion
  val specs2 = "org.specs2" %% "specs2-core" % specs2Version
  val specs2Mock = "org.specs2" %% "specs2-mock" % specs2Version
  val specs2Matchers = "org.specs2" %% "specs2-matcher" % specs2Version
  val specs2MatchersExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version
  val scalaTest =  "org.scalatest" %% "scalatest" % "3.2.15"
  val diff = "com.softwaremill.diffx" %% "diffx-scalatest" % "0.5.3"
  val localDynamoDB = "com.amazonaws" %% "DynamoDBLocal" % dynamoDbVersion
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
  val awsCloudWatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsClientVersion
  val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % awsClientVersion
  val okHttp = "com.squareup.okhttp3" % "okhttp" % "4.10.0"
  val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.7"
  val libPhoneNumber = "com.googlecode.libphonenumber" % "libphonenumber" % "8.13.4"
  val dynamoDB = "com.amazonaws" % "aws-java-sdk-dynamodb" % awsClientVersion
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.2"

  val dependencies = Seq(
    scalaUri,
    nscalaTime,
    akkaActor,
    supportInternationalisation,
    playJson,
    playJsonJoda,
    specs2 % "test",
    specs2Mock % "test",
    specs2Matchers % "test",
    specs2MatchersExtra % "test",
    scalaTest % "test",
    diff % "test",
    scalaLogging,
    awsCloudWatch,
    okHttp,
    scalaz,
    libPhoneNumber,
    dynamoDB,
    scalaXml
  )
}
