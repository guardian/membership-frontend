// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.16")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.3")

addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

addSbtPlugin("com.localytics" % "sbt-dynamodb" % "2.0.3") // Needed for membership-common

dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
