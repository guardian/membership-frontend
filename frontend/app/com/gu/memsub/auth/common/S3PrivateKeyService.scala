package com.gu.memsub.auth.common

import java.security.PrivateKey

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{GetObjectRequest, S3ObjectId}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.google.api.client.util.SecurityUtils.{loadPrivateKeyFromKeyStore, getPkcs12KeyStore => Pkcs12KeyStore}
import com.gu.memsub.auth.common.KeystoreCredentials.GoogleIssuedDefaults


class S3PrivateKeyService(credentialsProvider: AWSCredentialsProvider) {

  val s3Client: AmazonS3 = AmazonS3Client.builder
    .withCredentials(credentialsProvider)
    .withRegion(Regions.EU_WEST_1)
    .build()

  def loadPrivateKey(s3ObjectId: S3ObjectId, keyCreds: KeystoreCredentials = GoogleIssuedDefaults): PrivateKey = loadPrivateKeyFromKeyStore(
    Pkcs12KeyStore,
    s3Client.getObject(new GetObjectRequest(s3ObjectId)).getObjectContent,
    keyCreds.storePass,
    keyCreds.alias,
    keyCreds.keyPass
  )
}
