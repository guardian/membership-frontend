package com.gu

import com.amazonaws.auth.{InstanceProfileCredentialsProvider, AWSCredentialsProviderChain}
import com.amazonaws.auth.profile.ProfileCredentialsProvider

package object aws {
  val ProfileName = "membership"
  
  lazy val CredentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider(ProfileName),
    new InstanceProfileCredentialsProvider(false)
  )

}
