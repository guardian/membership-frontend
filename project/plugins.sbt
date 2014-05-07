// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.2")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.4.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")