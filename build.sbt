name := "membership-frontend"

lazy val root = (project in file(".")).aggregate(frontend)


val `membership-common` =
    (project in file("membership-common"))
        .enablePlugins(DynamoDBLocalPlugin)
        .settings(
            Seq(
                name := "membership-common",
                organization := "com.gu",
                scalaVersion := "2.13.10",
                scalacOptions := Seq("-feature", "-deprecation"),
                crossScalaVersions := Seq(scalaVersion.value),
                Compile / doc / sources := List(), // no docs please

                scmInfo := Some(
                    ScmInfo(
                        url("https://github.com/guardian/membership-common"),
                        "scm:git:git@github.com:guardian/membership-common.git",
                    ),
                ),
                description := "Scala library for common Guardian Membership/Subscriptions functionality.",
                licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
                resolvers ++= Seq(
                    "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
                    "turbolent" at "https://raw.githubusercontent.com/turbolent/mvn-repo/master/",
                ),
                dynamoDBLocalVersion := "2016-04-19",
                dynamoDBLocalDownloadDir := file("dynamodb-local"),
                startDynamoDBLocal := startDynamoDBLocal.dependsOn(Test / compile).value,
                Test / testQuick := (Test / testQuick).dependsOn(startDynamoDBLocal).evaluated,
                Test / test := (Test / test).dependsOn(startDynamoDBLocal).value,
                Test / testOptions += dynamoDBLocalTestCleanup.value,
                Compile / unmanagedResourceDirectories += baseDirectory.value / "conf",
                libraryDependencies ++= MembershipCommonDependencies.dependencies,
                dependencyOverrides += MembershipCommonDependencies.jacksonDatabind,
                ThisBuild / settingKey[String]("no need") := "There is no need to run `sbt release`, teamcity will automatically have released version 0.<build.counter> when you merged to the default branch",
            ),
        )

lazy val frontend = (project in file("frontend"))
    .enablePlugins(PlayScala, BuildInfoPlugin, RiffRaffArtifact)
    .dependsOn(`membership-common`)

