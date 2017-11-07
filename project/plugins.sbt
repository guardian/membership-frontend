// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.11")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.2")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.5")

addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "0.2.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.3")
