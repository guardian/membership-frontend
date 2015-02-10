package services

import com.gu.membership.stripe.StripeApiConfig
import services.zuora.ZuoraApiConfig


case class TouchpointBackendConfig(salesforce: SalesforceConfig, stripe: StripeApiConfig, zuora: ZuoraApiConfig)
