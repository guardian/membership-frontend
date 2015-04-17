package services

import java.io.{File, FileInputStream}

import com.amazonaws.regions.{Regions, Region}
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.SecurityUtils
import com.google.api.services.admin.directory.model.Group
import com.google.api.services.admin.directory.{Directory, DirectoryScopes}

import configuration.Config
import scala.collection.JavaConverters._
import scala.concurrent._

case class GoogleDirectoryConfig(
    serviceAccountId: String,
    serviceAccountEmail: String,
    serviceAccountCert: String)

object GoogleDirectoryService {

  val transport = new NetHttpTransport()
  val jsonFactory = new JacksonFactory()

  val credential = new GoogleCredential.Builder()
    .setTransport(transport)
    .setJsonFactory(jsonFactory)
    .setServiceAccountId(Config.googleDirectoryConfig.serviceAccountId)
    .setServiceAccountScopes(List(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY).asJavaCollection)
    .setServiceAccountUser(Config.googleDirectoryConfig.serviceAccountEmail)
    .setServiceAccountPrivateKey(loadServiceAccountPrivateKey)
    .build()

  val directory = new Directory.Builder(transport, jsonFactory, null)
    .setHttpRequestInitializer(credential)
    .build

  def retrieveGroupsFor(userEmail: String): Future[Set[String]] = {
    val query = directory.groups().list().setUserKey(userEmail)
    Future.successful {
      blocking {
        val groupsResponse = query.execute()
        groupsResponse.getGroups.asScala.toSet.map { group: Group => group.getEmail}
      }
    }
  }

  private def loadServiceAccountPrivateKey = {
    val awsRegion: Option[Region] = Option( Region getRegion Regions.EU_WEST_1 )

    val bucket = new S3BucketService(awsRegion, "membership-private")

    val certInputStream = bucket.getObjectInputStream(Config.googleDirectoryConfig.serviceAccountCert)
    val serviceAccountPrivateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
      SecurityUtils.getPkcs12KeyStore,
      certInputStream,
      "notasecret", "privatekey", "notasecret"
    )

    try { certInputStream.close() } catch { case _ : Throwable => }

    serviceAccountPrivateKey
  }
}
