package com.gu.memsub.auth.common

case class KeystoreCredentials(storePass: String, alias: String, keyPass: String)

object KeystoreCredentials {
  val GoogleIssuedDefaults = KeystoreCredentials("notasecret", "privatekey", "notasecret")
}
