package services

import services.zuora.ZuoraApiConfig


case class TouchpointBackendConfig(salesforce: SalesforceConfig, stripe: StripeApiConfig, zuora: ZuoraApiConfig)