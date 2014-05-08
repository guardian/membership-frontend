import sbt._
import sbt.Keys._
import play.Project._
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._

object PlayArtifact extends Plugin {

  val playArtifact = TaskKey[File]("play-artifact", "Builds a deployable zip file for magenta")
  val playArtifactResources = TaskKey[Seq[(File, String)]]("play-artifact-resources", "Files that will be collected by the deployment-artifact task")
  val playArtifactFile = SettingKey[String]("play-artifact-file", "Filename of the artifact built by deployment-artifact")

  val magentaPackageName = SettingKey[String]("magenta-package-name", "Name of the magenta package")

  lazy val playArtifactDistSettings = Seq(

    playArtifactResources := Seq(
      baseDirectory.value / (magentaPackageName.value + ".conf") ->
        ("packages/" + magentaPackageName.value + "/" + magentaPackageName.value + ".conf"),
      dist.value -> "packages/%s/%s".format(magentaPackageName.value, dist.value.getName),
      baseDirectory.value / "conf" / "deploy.json" -> "deploy.json"
    ),

    playArtifactFile := "artifacts.zip",
    playArtifact := {
      val distFile = target.value / playArtifactFile.value
      streams.value.log.info("Disting " + distFile)

      if (distFile.exists()) {
        distFile.delete()
      }
      IO.zip(playArtifactResources.value, distFile)

      // Tells TeamCity to publish the artifact => leave this println in here
      println("##teamcity[publishArtifacts '%s']".format(distFile))

      streams.value.log.info("Done disting.")
      distFile
    }
  )
}
