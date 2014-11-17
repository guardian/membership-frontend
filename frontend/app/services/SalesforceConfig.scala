package services

import com.netaporter.uri.Uri

case class SalesforceConfig(
  envName: String,
  consumerKey : String,
  consumerSecret: String,
  apiURL: Uri,
  apiUsername: String,
  apiPassword: String,
  apiToken: String
)
