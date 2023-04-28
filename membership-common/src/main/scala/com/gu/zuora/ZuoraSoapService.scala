package com.gu.zuora

import com.gu.memsub.Subscription._
import com.gu.memsub.{Subscription => S}
import com.gu.salesforce.ContactId
import com.gu.zuora.soap.Readers._
import com.gu.zuora.soap._
import com.gu.zuora.soap.models.{Queries => SoapQueries}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

object ZuoraSoapService {

  def latestInvoiceItems(items: Seq[SoapQueries.InvoiceItem]): Seq[SoapQueries.InvoiceItem] = {
    if(items.isEmpty)
      items
    else {
      val sortedItems = items.sortBy(_.chargeNumber)
      sortedItems.filter(_.subscriptionId == sortedItems.last.subscriptionId)
    }
  }
}

class ZuoraSoapService(soapClient: soap.ClientWithFeatureSupplier)(implicit ec: ExecutionContext) {
  implicit private val sc = soapClient

  def getAccountIds(contactId: ContactId): Future[List[AccountId]] =
    soapClient.query[SoapQueries.Account](SimpleFilter("crmId", contactId.salesforceAccountId))
      .map(_.map(a => AccountId(a.id)).toList)

   def getSubscription(id: S.Id): Future[SoapQueries.Subscription] =
    soapClient.queryOne[SoapQueries.Subscription](SimpleFilter("id", id.get))
}
