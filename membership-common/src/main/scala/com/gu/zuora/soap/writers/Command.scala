package com.gu.zuora.soap.writers
import com.github.nscala_time.time.Imports._
import com.gu.memsub.Subscription.AccountId
import com.gu.zuora.soap.models.Commands._
import org.joda.time.DateTime

import scala.xml.{Elem, NodeSeq}
import scalaz.Writer


object Command {

  def write[T](w: T => Elem): XmlWriter[T] = new XmlWriter[T] {
    override def write(t: T): Writer[Map[String, String], Elem] = Writer(Map.empty, w(t))
  }

  implicit val accountWrites: XmlWriter[Account] = write[Account] { t =>
    val invoiceTemplateId = t.invoiceTemplate.map(template => <ns2:InvoiceTemplateId>{template.id}</ns2:InvoiceTemplateId>) getOrElse NodeSeq.Empty

    <ns1:Account xsi:type="ns2:Account">
      <ns2:AutoPay>{t.autopay}</ns2:AutoPay>
      <ns2:BcdSettingOption>AutoSet</ns2:BcdSettingOption>
      <ns2:BillCycleDay>0</ns2:BillCycleDay>
      <ns2:Currency>{t.currency}</ns2:Currency>
      <ns2:Name>{t.contactId.salesforceAccountId}</ns2:Name>
      <ns2:PaymentTerm>Due Upon Receipt</ns2:PaymentTerm>
      <ns2:Batch>Batch1</ns2:Batch>
      <ns2:CrmId>{t.contactId.salesforceAccountId}</ns2:CrmId>
      <ns2:sfContactId__c>{t.contactId.salesforceContactId}</ns2:sfContactId__c>
      <ns2:IdentityId__c>{t.identityId}</ns2:IdentityId__c>
      <ns2:PaymentGateway>{t.paymentGateway.gatewayName}</ns2:PaymentGateway>
      {invoiceTemplateId}
    </ns1:Account>
}

    implicit val creditCardReferenceTransactionWrites = getCreditCardReferenceWrites(
      parent =  <ns1:PaymentMethod xsi:type="ns2:PaymentMethod"></ns1:PaymentMethod>,
      accountId = None
    )

  private def getCreditCardReferenceWrites(parent:Elem, accountId: Option[AccountId]): XmlWriter[CreditCardReferenceTransaction] = write[CreditCardReferenceTransaction] { t =>

    val accountIdLine = accountId.map(ac => <ns2:AccountId>{ac.get}</ns2:AccountId>) getOrElse NodeSeq.Empty
    val countryLine = t.cardCountry.map(c => <ns2:CreditCardCountry>{c.alpha2}</ns2:CreditCardCountry>) getOrElse NodeSeq.Empty

    val content =
      <placeholder>
        {accountIdLine}
        <ns2:TokenId>{t.cardId}</ns2:TokenId>
        <ns2:SecondTokenId>{t.customerId}</ns2:SecondTokenId>
        {countryLine}
        <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
        <ns2:CreditCardNumber>{t.last4}</ns2:CreditCardNumber>
        <ns2:CreditCardExpirationMonth>{t.expirationMonth}</ns2:CreditCardExpirationMonth>
        <ns2:CreditCardExpirationYear>{t.expirationYear}</ns2:CreditCardExpirationYear>
        <ns2:CreditCardType>{t.cardType.replaceAll(" ", "")}</ns2:CreditCardType>
    </placeholder>

    parent.copy(child = parent.child ++ content.child)
  }

private def getPaypalReferenceWrites(parent:Elem, accountId: Option[AccountId]): XmlWriter[PayPalReferenceTransaction] = write[PayPalReferenceTransaction] { t =>

  val accountIdLine = accountId.map(ac => <ns2:AccountId>{ac.get}</ns2:AccountId>) getOrElse NodeSeq.Empty

  val content =
    <placeholder>
      {accountIdLine}
      <ns2:PaypalBaid>{t.baId}</ns2:PaypalBaid>
      <ns2:PaypalEmail>{t.email}</ns2:PaypalEmail>
      <ns2:PaypalType>ExpressCheckout</ns2:PaypalType>
      <ns2:Type>PayPal</ns2:Type>
    </placeholder>

  parent.copy(child = parent.child ++ content.child)
}
  private def getBankTransferWrites(parent:Elem, accountId: Option[AccountId]): XmlWriter[BankTransfer] = write[BankTransfer] { t =>

    val accountIdLine = accountId.map(ac => <ns2:AccountId>{ac.get}</ns2:AccountId>) getOrElse NodeSeq.Empty

    val content =
      <placeholder>
          {accountIdLine}
          <ns2:Country>{t.countryCode}</ns2:Country>
          <ns2:BankTransferAccountName>{t.accountHolderName}</ns2:BankTransferAccountName>
          <ns2:BankTransferAccountNumber>{t.accountNumber}</ns2:BankTransferAccountNumber>
          <ns2:BankCode>{t.sortCode}</ns2:BankCode>
          <ns2:FirstName>{t.firstName}</ns2:FirstName>
          <ns2:LastName>{t.lastName}</ns2:LastName>
          <ns2:BankTransferType>DirectDebitUK</ns2:BankTransferType>
          <ns2:Type>BankTransfer</ns2:Type>
      </placeholder>

    parent.copy(child = parent.child ++ content.child)
  }

  implicit val bankTransferWrites = getBankTransferWrites(
      parent =  <ns1:PaymentMethod xsi:type="ns2:PaymentMethod"></ns1:PaymentMethod>,
      accountId = None
    )
  implicit val payPalReferenceTransactionWrites = getPaypalReferenceWrites(
    parent =  <ns1:PaymentMethod xsi:type="ns2:PaymentMethod"></ns1:PaymentMethod>,
    accountId = None
  )

  implicit val paymentMethodWrites = write[PaymentMethod] {
    case c: CreditCardReferenceTransaction => XmlWriter.write(c).value
    case b: BankTransfer => XmlWriter.write(b).value
    case p: PayPalReferenceTransaction => XmlWriter.write(p).value
  }

  implicit val createPaymentMethodWrites = write[CreatePaymentMethod] { t=>

    val zParent = <ns1:zObjects xsi:type="ns2:PaymentMethod"></ns1:zObjects>
    val someAccountId = Some(t.accountId)

    val bankTransferWrites = getBankTransferWrites(zParent, someAccountId)
    val creditCardReferenceTransactionWrites = getCreditCardReferenceWrites(zParent, someAccountId)
    val payPalReferenceTransactionWrites = getPaypalReferenceWrites(zParent, someAccountId)

    implicit val paymentMethodWrites = write[PaymentMethod] {
      case c: CreditCardReferenceTransaction => creditCardReferenceTransactionWrites.write(c).value
      case b: BankTransfer => bankTransferWrites.write(b).value
      case p: PayPalReferenceTransaction => payPalReferenceTransactionWrites.write(p).value
    }

    <ns1:create>
      {XmlWriter.write(t.paymentMethod).value}
    </ns1:create>
  }

  private def periodType(upToPeriodsType: PeriodType) = upToPeriodsType match {
    case Quarters => "Quarter"
    case Months => "Month"
    case Years => "Annual"
    case SingleYear => "Annual"
  }

  implicit val chargeOverrideWrites = write[ChargeOverride] { a =>
    <ns1:RatePlanChargeData>
      <ns1:RatePlanCharge>
        <ns2:ProductRatePlanChargeId>{a.productRatePlanChargeId}</ns2:ProductRatePlanChargeId>

        {a.discountPercentage.toSeq.map(dp =>
          <ns2:DiscountPercentage>{dp}</ns2:DiscountPercentage>
        )}

        {a.endDateCondition.toSeq.map( endDate => {
          endDate match {
            case SubscriptionEnd =>
              <ns2:EndDateCondition>SubscriptionEnd</ns2:EndDateCondition>
            case FixedPeriod(upToPeriods, upToPeriodsType) =>
              <ns2:UpToPeriods>{upToPeriods}</ns2:UpToPeriods>
              <ns2:UpToPeriodsType>{periodType(upToPeriodsType)}</ns2:UpToPeriodsType>
              <ns2:EndDateCondition>FixedPeriod</ns2:EndDateCondition>
            }
          }
        )}

        {a.triggerDate.toSeq.map(triggerDate =>
          <ns2:TriggerEvent>SpecificDate</ns2:TriggerEvent>
          <ns2:TriggerDate>{triggerDate}</ns2:TriggerDate>
        )}

        {a.billingPeriod.toSeq.map(bp =>
          <ns2:BillingPeriod>{periodType(bp)}</ns2:BillingPeriod>
        )}

        {a.price.toSeq.map( p => <ns2:Price>{p}</ns2:Price>)}
      </ns1:RatePlanCharge>
    </ns1:RatePlanChargeData>
  }

  implicit val ratePlanWrites = write[RatePlan] { t =>
    <ns1:RatePlanData>
      <ns1:RatePlan xsi:type="ns2:RatePlan">
        <ns2:ProductRatePlanId>{t.productRatePlanId}</ns2:ProductRatePlanId>
      </ns1:RatePlan>
      {t.chargeOverride.toSeq.map(a => XmlWriter.write(a).value)}
      <ns1:SubscriptionProductFeatureList>
        {t.featureIds.map(id =>
        <ns1:SubscriptionProductFeature xsi:type="ns2:SubscriptionProductFeature">
          <ns2:FeatureId>{id}</ns2:FeatureId>
        </ns1:SubscriptionProductFeature>
      )}
      </ns1:SubscriptionProductFeatureList>
    </ns1:RatePlanData>
  }

  implicit val soldToContactWrites = write[SoldToContact] { contact =>
    <ns1:SoldToContact xsi:type="ns2:Contact">
      <ns2:FirstName>{contact.name.first}</ns2:FirstName>
      <ns2:LastName>{contact.name.last}</ns2:LastName>
      <ns2:Address1>{contact.address.lineOne}</ns2:Address1>
      <ns2:Address2>{contact.address.lineTwo}</ns2:Address2>
      <ns2:City>{contact.address.town}</ns2:City>
      <ns2:PostalCode>{contact.address.postCode}</ns2:PostalCode>
      <ns2:State>{contact.address.countyOrState}</ns2:State>
      <ns2:Country>{contact.address.country.fold(contact.address.countryName)(_.alpha2)}</ns2:Country>
      {
      contact.email.map(email =>
        <ns2:WorkEmail>{email}</ns2:WorkEmail>
      ).getOrElse(NodeSeq.Empty)}
      {
      contact.deliveryInstructions.map(instructions =>
        <ns2:SpecialDeliveryInstructions__c>{instructions}</ns2:SpecialDeliveryInstructions__c>
      ).getOrElse(NodeSeq.Empty)}
      {
      contact.phone.map(phone =>
        <ns2:WorkPhone>{phone.format}</ns2:WorkPhone>
      ).getOrElse(NodeSeq.Empty)}
      {
      contact.name.title.map(title =>
        <ns2:Title__c>{title.title}</ns2:Title__c>
      ).getOrElse(NodeSeq.Empty)
      }
    </ns1:SoldToContact>
  }

  implicit val subscribeWrites = new XmlWriter[Subscribe] {

    override def write(command: Subscribe): Writer[Map[String, String], Elem] = {

      val now = DateTime.now
      val soldToContactNode = command.soldToContact.fold[NodeSeq](NodeSeq.Empty)(soldToContact => XmlWriter.write(soldToContact).value)
      val ipCountryNode = command.ipCountry.fold[NodeSeq](NodeSeq.Empty)(c => <ns2:IPCountry__c>{c.alpha2}</ns2:IPCountry__c>)
      val paymentNode = command.paymentMethod.fold[NodeSeq](NodeSeq.Empty)(h => XmlWriter.write(h).value)
      val promotionCodeNode = command.promoCode.fold[NodeSeq](NodeSeq.Empty)(code =>
        <ns2:InitialPromotionCode__c>{code.get}</ns2:InitialPromotionCode__c>
        <ns2:PromotionCode__c>{code.get}</ns2:PromotionCode__c>
      )
      val supplierCodeNode = command.supplierCode.fold[NodeSeq](NodeSeq.Empty)(code => <ns2:SupplierCode__c>{code.get}</ns2:SupplierCode__c>)

      Writer(
        Map(
          "Salesforce ID" -> command.account.contactId.salesforceContactId,
          "Rate plan ID" -> command.ratePlans.head.productRatePlanId,
          "Current time" -> s"$now in sunny ${now.getZone.getID}",
          "Customer acceptance" -> command.contractAcceptance.toString,
          "Contract effective" -> command.contractEffective.toString
        ),
      <ns1:subscribe>
        <ns1:subscribes>
          {XmlWriter.write(command.account).value}{paymentNode}
          <ns1:BillToContact xsi:type="ns2:Contact">
            <ns2:FirstName>{command.name.first}</ns2:FirstName>
            <ns2:LastName>{command.name.last}</ns2:LastName>
            <ns2:Address1>{command.address.lineOne}</ns2:Address1>
            <ns2:Address2>{command.address.lineTwo}</ns2:Address2>
            <ns2:City>{command.address.town}</ns2:City>
            <ns2:PostalCode>{command.address.postCode}</ns2:PostalCode>
            <ns2:State>{command.address.countyOrState}</ns2:State>
            <ns2:Country>{command.address.country.fold(command.address.countryName)(_.alpha2)}</ns2:Country>
            <ns2:WorkEmail>{command.email}</ns2:WorkEmail>
            {
            command.phone.map(phone =>
              <ns2:WorkPhone>{phone.format}</ns2:WorkPhone>
            ).getOrElse(NodeSeq.Empty)
            }{
            command.name.title.map(title =>
              <ns2:Title__c>{title.title}</ns2:Title__c>
            ).getOrElse(NodeSeq.Empty)
            }
          </ns1:BillToContact>
          <ns1:PreviewOptions>
            <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
            <ns1:NumberOfPeriods>1</ns1:NumberOfPeriods>
          </ns1:PreviewOptions>
          {soldToContactNode}
          <ns1:SubscribeOptions>
            <ns1:GenerateInvoice>true</ns1:GenerateInvoice>
            <ns1:ProcessPayments>true</ns1:ProcessPayments>
          </ns1:SubscribeOptions>
          <ns1:SubscriptionData>
            <ns1:Subscription xsi:type="ns2:Subscription">
              <ns2:AutoRenew>true</ns2:AutoRenew>
              <ns2:ContractEffectiveDate>{command.contractEffective}</ns2:ContractEffectiveDate>
              <ns2:ContractAcceptanceDate>{command.contractAcceptance}</ns2:ContractAcceptanceDate>
              <ns2:InitialTerm>12</ns2:InitialTerm>
              <ns2:RenewalTerm>12</ns2:RenewalTerm>
              <ns2:TermStartDate>{command.contractEffective}</ns2:TermStartDate>
              <ns2:TermType>TERMED</ns2:TermType>
              <ns2:ReaderType__c>{command.readerType.value}</ns2:ReaderType__c>
              {promotionCodeNode}
              {supplierCodeNode}
              {ipCountryNode}
            </ns1:Subscription>{command.ratePlans.list.toList.map(ratePlan => XmlWriter.write(ratePlan).value)}
          </ns1:SubscriptionData>
        </ns1:subscribes>
      </ns1:subscribe>
    )}
  }

  implicit val contributeWrites = new XmlWriter[Contribute] {

    override def write(t: Contribute): Writer[Map[String, String], Elem] = {

      val now = DateTime.now
      val paymentNode = t.paymentMethod.fold[NodeSeq](NodeSeq.Empty)(h => XmlWriter.write(h).value)

      Writer(
        Map(
          "Salesforce ID" -> t.account.contactId.salesforceContactId,
          "Rate plan ID" -> t.ratePlans.head.productRatePlanId,
          "Current time" -> s"$now in sunny ${now.getZone.getID}",
          "Customer acceptance" -> t.contractAcceptance.toString,
          "Contract effective" -> t.contractEffective.toString
        ),
        <ns1:subscribe>
          <ns1:subscribes>
            {XmlWriter.write(t.account).value}{paymentNode}
            <ns1:BillToContact xsi:type="ns2:Contact">
              <ns2:FirstName>{t.name.first}</ns2:FirstName>
              <ns2:LastName>{t.name.last}</ns2:LastName>
              <ns2:Country>{t.country}</ns2:Country>
              <ns2:WorkEmail>{t.email}</ns2:WorkEmail>
            </ns1:BillToContact>
            <ns1:PreviewOptions>
              <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
              <ns1:NumberOfPeriods>1</ns1:NumberOfPeriods>
            </ns1:PreviewOptions>
            <ns1:SubscribeOptions>
              <ns1:GenerateInvoice>true</ns1:GenerateInvoice>
              <ns1:ProcessPayments>true</ns1:ProcessPayments>
            </ns1:SubscribeOptions>
            <ns1:SubscriptionData>
              <ns1:Subscription xsi:type="ns2:Subscription">
                <ns2:AutoRenew>true</ns2:AutoRenew>
                <ns2:ContractEffectiveDate>{t.contractEffective}</ns2:ContractEffectiveDate>
                <ns2:ContractAcceptanceDate>{t.contractAcceptance}</ns2:ContractAcceptanceDate>
                <ns2:InitialTerm>12</ns2:InitialTerm>
                <ns2:RenewalTerm>12</ns2:RenewalTerm>
                <ns2:TermStartDate>{t.contractEffective}</ns2:TermStartDate>
                <ns2:TermType>TERMED</ns2:TermType>
              </ns1:Subscription>{t.ratePlans.list.toList.map(ratePlan => XmlWriter.write(ratePlan).value)}
            </ns1:SubscriptionData>
          </ns1:subscribes>
        </ns1:subscribe>
      )}
  }

  implicit val updatePromoCodeWrites = write[UpdatePromoCode] { t =>
    <ns1:update>
      <ns1:zObjects xsi:type="ns2:Subscription">
        <ns2:Id>{t.subscriptionId}</ns2:Id>
        <ns2:PromotionCode__c>{t.promoCode}</ns2:PromotionCode__c>
      </ns1:zObjects>
    </ns1:update>
  }

  implicit val amendWrites = write[Amend] { t =>
    val dateStr = DateTime.now().toLocalDate
    <ns1:amend>
      <ns1:requests>
        {t.plansToRemove.map(oldPlan =>
          <ns1:Amendments>
            <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
            <ns2:CustomerAcceptanceDate>{dateStr}</ns2:CustomerAcceptanceDate>
            <ns2:Name>Upgrade</ns2:Name>
            <ns2:RatePlanData>
              <ns1:RatePlan>
                <ns2:AmendmentSubscriptionRatePlanId>{oldPlan}</ns2:AmendmentSubscriptionRatePlanId>
              </ns1:RatePlan>
            </ns2:RatePlanData>
            <ns2:Status>Completed</ns2:Status>
            <ns2:SubscriptionId>{t.subscriptionId}</ns2:SubscriptionId>
            <ns2:Type>RemoveProduct</ns2:Type>
          </ns1:Amendments>
        )}
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
          <ns2:CustomerAcceptanceDate>{dateStr}</ns2:CustomerAcceptanceDate>
          <ns2:EffectiveDate>{dateStr}</ns2:EffectiveDate>
          <ns2:Name>Upgrade</ns2:Name>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{t.subscriptionId}</ns2:SubscriptionId>
          <ns2:TermStartDate>{dateStr}</ns2:TermStartDate>
          <ns2:Type>TermsAndConditions</ns2:Type>
        </ns1:Amendments>
        {t.newRatePlans.list.toList.map(plan =>
          <ns1:Amendments>
            <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
            <ns2:CustomerAcceptanceDate>{dateStr}</ns2:CustomerAcceptanceDate>
            <ns2:Name>Upgrade</ns2:Name>
            {XmlWriter.write(plan).value}
            <ns2:Status>Completed</ns2:Status>
            <ns2:SubscriptionId>{t.subscriptionId}</ns2:SubscriptionId>
            <ns2:Type>NewProduct</ns2:Type>
          </ns1:Amendments>
        )}
        <ns1:AmendOptions>
          <ns1:GenerateInvoice>true</ns1:GenerateInvoice>Rat
          <ns1:InvoiceProcessingOptions>
            <ns1:InvoiceTargetDate>{dateStr}</ns1:InvoiceTargetDate>
          </ns1:InvoiceProcessingOptions>
          <ns1:ProcessPayments>true</ns1:ProcessPayments>
        </ns1:AmendOptions>
        <ns1:PreviewOptions>
          <ns1:EnablePreviewMode>{t.previewMode}</ns1:EnablePreviewMode>
          <ns1:PreviewThroughTermEnd>true</ns1:PreviewThroughTermEnd>
        </ns1:PreviewOptions>
      </ns1:requests>
    </ns1:amend>
  }

  implicit val renewWrites = write[Renew] { renewCommand =>
    val now = LocalDate.now

    val autoRenewAmendment = if (renewCommand.autoRenew) {
      <ns1:Amendments>
        <ns2:ContractEffectiveDate>{now}</ns2:ContractEffectiveDate>
        <ns2:CustomerAcceptanceDate>{now}</ns2:CustomerAcceptanceDate>
        <ns2:AutoRenew>true</ns2:AutoRenew>
        <ns2:Name>setAutoRenew</ns2:Name>
        <ns2:Status>Completed</ns2:Status>
        <ns2:SubscriptionId>{renewCommand.subscriptionId}</ns2:SubscriptionId>
        <ns2:Type>TermsAndConditions</ns2:Type>
      </ns1:Amendments>
    } else {
      NodeSeq.Empty
    }

    val editTermDatesAmendment = if (renewCommand.fastForwardTermStartDate) {
      <ns1:Amendments>
        <ns2:ContractEffectiveDate>{now}</ns2:ContractEffectiveDate>
        <ns2:CustomerAcceptanceDate>{now}</ns2:CustomerAcceptanceDate>
        <ns2:Name>editTermDates</ns2:Name>
        <ns2:Status>Completed</ns2:Status>
        <ns2:SubscriptionId>{renewCommand.subscriptionId}</ns2:SubscriptionId>
        <ns2:TermStartDate>{renewCommand.newTermStartDate}</ns2:TermStartDate>
        <ns2:Type>TermsAndConditions</ns2:Type>
      </ns1:Amendments>
    } else {
      NodeSeq.Empty
    }

    <ns1:amend>
      <ns1:requests>
       <ns1:Amendments>
          <ns2:ContractEffectiveDate>{renewCommand.currentTermEndDate}</ns2:ContractEffectiveDate>
          <ns2:CustomerAcceptanceDate>{renewCommand.currentTermEndDate}</ns2:CustomerAcceptanceDate>
          <ns2:Name>removeOldPlan</ns2:Name>
          <ns2:RatePlanData>
            <ns1:RatePlan>
              <ns2:AmendmentSubscriptionRatePlanId>{renewCommand.planToRemove}</ns2:AmendmentSubscriptionRatePlanId>
            </ns1:RatePlan>
          </ns2:RatePlanData>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{renewCommand.subscriptionId}</ns2:SubscriptionId>
          <ns2:Type>RemoveProduct</ns2:Type>
       </ns1:Amendments>
       {autoRenewAmendment}
        <ns1:Amendments>
          <ns2:Name>renew</ns2:Name>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{renewCommand.subscriptionId}</ns2:SubscriptionId>
          <ns2:ContractEffectiveDate>{renewCommand.currentTermEndDate}</ns2:ContractEffectiveDate>
          <ns2:CustomerAcceptanceDate>{renewCommand.currentTermEndDate}</ns2:CustomerAcceptanceDate>
          <ns2:Type>Renewal</ns2:Type>
        </ns1:Amendments>
        {editTermDatesAmendment}
        {renewCommand.newRatePlans.list.toList.map(plan =>
         <ns1:Amendments>
          <ns2:ContractEffectiveDate>{renewCommand.newTermStartDate}</ns2:ContractEffectiveDate>
          <ns2:CustomerAcceptanceDate>{renewCommand.newTermStartDate}</ns2:CustomerAcceptanceDate>
          <ns2:Name>addRatePlan</ns2:Name>
          {XmlWriter.write(plan).value}
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{renewCommand.subscriptionId}</ns2:SubscriptionId>
          <ns2:Type>NewProduct</ns2:Type>
        </ns1:Amendments>
      )}
      </ns1:requests>
    </ns1:amend>
  }
}
