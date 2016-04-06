package configuration

case class LegacyRatePlanIds(friend: String,
                             supporterMonthly: String,
                             supporterYearly: String,
                             partnerMonthly: String,
                             partnerYearly: String,
                             patronMonthly: String,
                             patronYearly: String) {

  val ids = Set(
      friend,
      supporterMonthly,
      supporterYearly,
      partnerMonthly,
      partnerYearly,
      patronMonthly,
      patronYearly
  )
}

object LegacyRatePlanIds {
  def fromConfig(config: com.typesafe.config.Config) =
    LegacyRatePlanIds(
        friend = config.getString("friend"),
        supporterMonthly = config.getString("supporter.monthly"),
        supporterYearly = config.getString("supporter.yearly"),
        partnerMonthly = config.getString("partner.monthly"),
        partnerYearly = config.getString("partner.yearly"),
        patronMonthly = config.getString("patron.monthly"),
        patronYearly = config.getString("patron.yearly")
    )
}
