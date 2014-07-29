package views.support

import com.gu.membership.salesforce.Tier

object Options {
  def annualAmount(targetTier: Tier.Tier): String = {
    if (targetTier == Tier.Partner) {
      "£135 one off payment (save £45 a year)"
    } else {
      "£540 one off payment (save £180 a year)"
    }
  }
  def monthAmount(targetTier: Tier.Tier): String = {
    if(targetTier == Tier.Partner) {
      "£15 per month (£180 per year)"
    } else {
      "£60 per month (£720 per year)"
    }
  }
}
