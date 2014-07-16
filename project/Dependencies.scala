import sbt._
import play._

object Dependencies {

  //versions
  val nScalaTimeVersion = "1.0.0"
  val akkaVersion = "2.0.0"
  val identityCookieVersion = "3.4.0"
  val identityModelVersion = "3.4.0"
  val awsScalaVersion = "0.2.1"
  val scalaUriVersion ="0.4.1"

  //libraries
  val nScalaTime = "com.github.nscala-time" %% "nscala-time" % nScalaTimeVersion
  val akkaAgent = "com.typesafe.akka" %% "akka-agent" % akkaVersion
  val identityCookie = "com.gu.identity" %% "identity-cookie" % identityCookieVersion
  val identityModel = "com.gu.identity" %% "identity-model" % identityModelVersion
  val awsScala = "com.github.seratch" %% "awscala" % awsScalaVersion
  val scalaUri = "com.netaporter" %% "scala-uri" % scalaUriVersion
  val play = PlayImport.ws

  //projects
  val frontendDependencies = Seq(nScalaTime, akkaAgent, identityCookie, identityModel, awsScala, scalaUri, play)
}
