package services

import java.security.PrivateKey
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{InstanceProfileCredentialsProvider, AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain, AWSCredentials}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.google.api.client.util.SecurityUtils

class S3BucketService(regionOption: Option[Region] = None, bucketName: String, credentialsProvider: AWSCredentialsProviderChain) {

  val region = regionOption getOrElse(Region getRegion(Regions.EU_WEST_1))
  val s3Client = region.createClient(classOf[AmazonS3Client], credentialsProvider, new ClientConfiguration())

  def getObjectInputStream(objectPath: String) = {
    val s3File = s3Client.getObject(new GetObjectRequest(bucketName, objectPath))
    s3File.getObjectContent
  }

  def loadServiceAccountPrivateKeyS3(path: String, storePass: String, alias: String, keyPass: String): PrivateKey  = {
    val certInputStream = this.getObjectInputStream(path)

    val serviceAccountPrivateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
      SecurityUtils.getPkcs12KeyStore,
      certInputStream,
      storePass,
      alias,
      keyPass
    )

    try { certInputStream.close() } catch { case _ : Throwable => }

    serviceAccountPrivateKey
  }
}
