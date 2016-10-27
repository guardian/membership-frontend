import Dependencies._

def env(key: String, default: String): String = Option(System.getenv(key)).getOrElse(default)

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
    name,
    BuildInfoKey.constant("buildNumber", Option(System.getenv("BUILD_NUMBER")) getOrElse "DEV"),
    BuildInfoKey.constant("buildTime", System.currentTimeMillis),
    BuildInfoKey.constant("gitCommitId", Option(System.getenv("BUILD_VCS_NUMBER")) getOrElse(try {
        "git rev-parse HEAD".!!.trim
    } catch {
        case e: Exception => "unknown"
    }))
)

buildInfoPackage := "app"

organization := "com.gu"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers ++= Seq(
    "Guardian Github Releases" at "https://guardian.github.io/maven/repo-releases",
    "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-snapshots",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    Resolver.sonatypeRepo("releases"))

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

parallelExecution in Global := false

updateOptions := updateOptions.value.withCachedResolution(true)

assemblyMergeStrategy in assembly := { // We only use sbt-assembly as a canary to tell us about clashes - we DON'T use the generated uber-jar, instead the native-packaged zip
    case x if x.startsWith("com/google/gdata/util/common/base/") => MergeStrategy.first // https://github.com/playframework/playframework/issues/3365#issuecomment-198399016
    case "version.txt"                                           => MergeStrategy.discard // identity jars all include version.txt
    case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
}

test in assembly := {} // skip tests during assembly

javaOptions in Test += "-Dconfig.file=test/acceptance/conf/acceptance-test.conf"

testOptions in Test += Tests.Argument("-oD") // display execution times in Scalatest output

useNativeZip

riffRaffPackageType := (packageZipTarball in Universal).value

riffRaffBuildIdentifier := env("BUILD_NUMBER", "DEV")

riffRaffManifestBranch := env("BRANCH_NAME", "unknown_branch")

riffRaffManifestVcsUrl  := "git@github.com:guardian/membership-frontend.git"

riffRaffUploadArtifactBucket := Option("riffraff-artifact")

riffRaffUploadManifestBucket := Option("riffraff-builds")


play.sbt.routes.RoutesKeys.routesImport ++= Seq(
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

addCommandAlias("devrun", "run -Dconfig.resource=dev.conf 9100")

libraryDependencies ++= acceptanceTestDependencies

addCommandAlias("fast-test", "testOnly -- -l Acceptance")

addCommandAlias("acceptance-test", "testOnly acceptance.JoinPartnerSpec")
