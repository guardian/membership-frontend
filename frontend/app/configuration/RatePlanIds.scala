package configuration

case class RatePlanIds(
  friend: String,
  staff: String,
  supporterMonthly: String,
  supporterYearly: String,
  partnerMonthly: String,
  partnerYearly: String,
  patronMonthly: String,
  patronYearly: String,
  legacy: LegacyRatePlanIds) {

  val ids = Set(
    friend,
    staff,
    supporterMonthly,
    supporterYearly,
    partnerMonthly,
    partnerYearly,
    patronMonthly,
    patronYearly
  ) ++ legacy.ids
}

object RatePlanIds {
  def fromConfig(config: com.typesafe.config.Config) =
    RatePlanIds(
      friend = config.getString("friend"),
      staff = config.getString("staff"),
      supporterMonthly = config.getString("supporter.monthly"),
      supporterYearly = config.getString("supporter.yearly"),
      partnerMonthly = config.getString("partner.monthly"),
      partnerYearly = config.getString("partner.yearly"),
      patronMonthly = config.getString("patron.monthly"),
      patronYearly = config.getString("patron.yearly"),
      legacy = LegacyRatePlanIds.fromConfig(config.getConfig("legacy"))
    )
}
