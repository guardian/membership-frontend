package com.gu.zuora.soap.actions

import com.gu.zuora.ZuoraSoapConfig
import com.gu.zuora.soap.models.Results._

object Actions {

  /*
   * TODO: Split up these actions into simple models (In models/Commands) and XmlWriters
   */

  case class Query(query: String, enableLog: Boolean = true) extends Action[QueryResult] {
    override def additionalLogInfo = Map("Query" -> query)
    val body =
      <ns1:query>
        <ns1:queryString>{query}</ns1:queryString>
      </ns1:query>
    override val enableLogging = enableLog
  }
  case class Login(apiConfig: ZuoraSoapConfig) extends Action[Authentication] {
    override val authRequired = false
    val body =
      <api:login>
        <api:username>{apiConfig.username}</api:username>
        <api:password>{apiConfig.password}</api:password>
      </api:login>
    override def sanitized = "<api:login>...</api:login>"
  }

}
