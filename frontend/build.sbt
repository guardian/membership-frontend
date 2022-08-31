import scala.sys.process._
import Dependencies._

def env(key: String, default: String): String = Option(System.getenv(key)).getOrElse(default)

def commitId(): String = try {
    "git rev-parse HEAD".!!.trim
} catch {
    case _: Exception => "unknown"
}

buildInfoKeys := Seq[BuildInfoKey](
    name,
    BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
    BuildInfoKey.constant("buildTime", System.currentTimeMillis),
    BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(commitId()))
)

buildInfoOptions += BuildInfoOption.ToMap

buildInfoPackage := "app"

organization := "com.gu"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.8"

resolvers ++= Seq(
    "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
    Resolver.sonatypeRepo("releases")
)

Compile / doc / sources := Seq.empty

Compile / packageDoc / publishArtifact := false

Global / parallelExecution := false

updateOptions := updateOptions.value.withCachedResolution(true)

assembly / assemblyMergeStrategy := { // We only use sbt-assembly as a canary to tell us about clashes - we DON'T use the generated uber-jar, instead the native-packaged zip
    case x if x.startsWith("com/google/gdata/util/common/base/") => MergeStrategy.first // https://github.com/playframework/playframework/issues/3365#issuecomment-198399016
    case "version.txt"                                           => MergeStrategy.discard // identity jars all include version.txt
    case "shared.thrift" => MergeStrategy.first
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case "play/reference-overrides.conf" => MergeStrategy.concat
    case PathList("ahc-version.properties") => MergeStrategy.first
    case PathList("ahc-default.properties") => MergeStrategy.concat
    case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
}

assembly / test := {} // skip tests during assembly

Test / javaOptions += "-Dconfig.file=test/conf/test.conf"

Test / testOptions += Tests.Argument("-oD") // display execution times in Scalatest output


enablePlugins(SystemdPlugin)

debianPackageDependencies := Seq("openjdk-8-jre-headless")

Universal / javaOptions ++= Seq(
    "-Dpidfile.path=/dev/null",
    "-J-XX:MaxRAMFraction=2",
    "-J-XX:InitialRAMFraction=2",
    "-J-XX:MaxMetaspaceSize=500m",
    "-J-XX:+PrintGCDetails",
    "-J-XX:+PrintGCDateStamps",
    s"-J-Xloggc:/var/log/${name.value}/gc.log"
)

maintainer := "Membership Dev <membership.dev@theguardian.com>"

packageSummary := "Membership Frontend service"

packageDescription := """Membership Frontend appserver for https://membership.theguardian.com"""

riffRaffPackageType := (Debian / packageBin).value

riffRaffBuildIdentifier := env("BUILD_NUMBER", "DEV")

riffRaffManifestBranch := env("BRANCH_NAME", "unknown_branch")

riffRaffManifestVcsUrl  := "git@github.com:guardian/membership-frontend.git"

riffRaffUploadArtifactBucket := Option("riffraff-artifact")

riffRaffUploadManifestBucket := Option("riffraff-builds")

riffRaffArtifactResources += (file("cloud-formation/membership-app.cf.yaml"), "cfn/cfn.yaml")

play.sbt.routes.RoutesKeys.routesImport ++= Seq(
    "utils.{Feature,OnOrOff}",
    "com.gu.salesforce.Tier",
    "com.gu.salesforce.FreeTier",
    "com.gu.salesforce.PaidTier",
    "com.gu.i18n.Country",
    "com.gu.i18n.CountryGroup",
    "com.gu.memsub.Subscription.ProductRatePlanId",
    "com.gu.memsub.promo.PromoCode",
    "controllers.Binders._",
    "com.gu.memsub.BillingPeriod"
)

libraryDependencies ++= frontendDependencies.map {
    _.exclude("commons-logging", "commons-logging") // our dependencies include jcl-over-slf4j http://www.slf4j.org/legacy.html#jcl-over-slf4j
     .exclude("org.slf4j", "slf4j-simple") // snatches SLF4J logging from Logback, our chosen logging system
}

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion

addCommandAlias("devrun", "run 9100")
