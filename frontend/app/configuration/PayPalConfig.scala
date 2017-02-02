package configuration

import com.typesafe.config.Config

case class PayPalConfig(payPalNVPVersion: String,
                        payPalUrl: String,
                        payPalUser: String,
                        payPalPassword: String,
                        payPalSignature: String)

object PayPalConfig {
  def fromConfig(config: Config): PayPalConfig = {
    PayPalConfig(
      config.getString("nvp-version"),
      config.getString("url"),
      config.getString("user"),
      config.getString("password"),
      config.getString("signature")
    )
  }

}
