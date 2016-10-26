lazy val root = (project in file(".")).aggregate(frontend)

lazy val frontend = (project in file("frontend")).enablePlugins(PlayScala, RiffRaffArtifact)

