package com.gu.config
import com.gu.memsub.Subscription.ProductId
import com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds

object SubsV2ProductIds {
  def apply(config: com.typesafe.config.Config): ProductIds = ProductIds(
    weeklyZoneA = ProductId(config.getString("subscriptions.weeklyZoneA")),
    weeklyZoneB = ProductId(config.getString("subscriptions.weeklyZoneB")),
    weeklyZoneC = ProductId(config.getString("subscriptions.weeklyZoneC")),
    weeklyDomestic = ProductId(config.getString("subscriptions.weeklyDomestic")),
    weeklyRestOfWorld = ProductId(config.getString("subscriptions.weeklyRestOfWorld")),
    delivery = ProductId(config.getString("subscriptions.delivery")),
    voucher = ProductId(config.getString("subscriptions.voucher")),
    digitalVoucher = ProductId(config.getString("subscriptions.digitalVoucher")),
    digipack = ProductId(config.getString("subscriptions.digipack")),
    supporterPlus = ProductId(config.getString("subscriptions.supporterPlus")),
    friend = ProductId(config.getString("membership.friend")),
    supporter = ProductId(config.getString("membership.supporter")),
    partner = ProductId(config.getString("membership.partner")),
    patron = ProductId(config.getString("membership.patron")),
    staff = ProductId(config.getString("membership.staff")),
    contributor = ProductId(config.getString("contributions.contributor"))
  )
}
