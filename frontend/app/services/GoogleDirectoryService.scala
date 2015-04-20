package services

import java.security.PrivateKey

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.admin.directory.model.Group
import com.google.api.services.admin.directory.{Directory, DirectoryScopes}

import scala.collection.JavaConverters._
import scala.concurrent._

case class GoogleDirectoryConfig(
    serviceAccountId: String,
    serviceAccountEmail: String,
    serviceAccountCert: String,
    storePass: String,
    alias: String,
    keyPass: String)

class GoogleDirectoryService(config: GoogleDirectoryConfig, privateKey: PrivateKey) {

  val transport = new NetHttpTransport()
  val jsonFactory = new JacksonFactory()

  val credential = new GoogleCredential.Builder()
    .setTransport(transport)
    .setJsonFactory(jsonFactory)
    .setServiceAccountId(config.serviceAccountId)
    .setServiceAccountScopes(List(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY).asJavaCollection)
    .setServiceAccountUser(config.serviceAccountEmail)
    .setServiceAccountPrivateKey(privateKey)
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
}
