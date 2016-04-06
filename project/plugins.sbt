// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.6")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")

addSbtPlugin("com.geirsson" %% "sbt-scalafmt" % "0.1.6")
