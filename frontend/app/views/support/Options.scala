package views.support

import model.Tier

object Options {
  def annualAmount(targetTier: model.Tier.Tier): String = {
    if (targetTier == Tier.Partner) {
      "£135 one off payment (save £45 a year)"
    } else {
      "£540 one off payment (save £180 a year)"
    }
  }
  def monthAmount(targetTier: model.Tier.Tier): String = {
    if(targetTier == Tier.Partner) {
      "£15 per month (£180 per year)"
    } else {
      "£60 per month (£720 per year)"
    }
  }
}
