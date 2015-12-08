package views.support

import com.gu.membership.salesforce.Tier
import model.TicketSaleDates

object TicketSaleCTA {

  val PossibleTiers = Tier.all.map(Some(_)) :+ None

  /**
    * Get the Call-To-Action that occurs on the buy-ticket button. This info is written out to the cached Event Details
    * page in data attributes, which are then used by client-side javascript (which knows what the tier of the active
    * user is) to display the appropriate CTA.
    *
    * Rules for the CTA
    * - If event is not on sale to anyone return 'unavailable' status object for ALL users
    * - If event is within priority booking mode and user is not logged-in return 'join' status object
    * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the
    * future return 'upgrade' status object
    * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the
    * past return empty 'buy' status object
    * - If event is on sale to everyone, and user is logged-in with or without a tier return empty 'buy' status object
    *
    */
  def ctaFor(dates: TicketSaleDates, tierOpt: Option[Tier]): String = if (dates.noOneCanBuyTicket) "unavailable" else {
    tierOpt.map(t => if (dates.tierCanBuyTicket(t)) "buy" else "upgrade").getOrElse("join")
  }
}
