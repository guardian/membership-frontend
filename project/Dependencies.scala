import sbt._
import play._

object Dependencies {

  val identity = "3.40"

  //libraries
  val nScalaTime = "com.github.nscala-time" %% "nscala-time" % "1.0.0"
  val akkaAgent = "com.typesafe.akka" %% "akka-agent" % "2.2.0"
  val identityCookie = "com.gu.identity" %% "identity-cookie" % identity
  val identityModel = "com.gu.identity" %% "identity-model" % identity
  val awsScala = "com.github.seratch" %% "awscala" % "0.2.1"
  val scalaUri = "com.netaporter" %% "scala-uri" % "0.4.1"
  val playWS = PlayImport.ws

  //projects
  val frontendDependencies = Seq(nScalaTime, akkaAgent, identityCookie, identityModel, awsScala, scalaUri, playWS)
}
