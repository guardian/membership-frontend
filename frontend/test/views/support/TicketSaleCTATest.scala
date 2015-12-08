package views.support

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier._
import model.TicketSaleDates
import org.specs2.mutable.Specification
import views.support.TicketSaleCTA.ctaFor


class TicketSaleCTATest extends Specification {

  val eventDate = DateTime.now + 1.month

  "cta" should {
    "be unavailable if the tickets not yet on sale for anyone" in {
      val dates = TicketSaleDates((eventDate - 10.days).toInstant, Some(Map(Tier.Patron -> (eventDate - 12.days).toInstant)))

      ctaFor(dates, None) must be("unavailable")
      ctaFor(dates, Some(Supporter)) must be("unavailable")
      ctaFor(dates, Some(Patron)) must be("unavailable")
    }

    "encourage joining or upgrading membership if only priority booking is available" in {
      val dates = TicketSaleDates((eventDate - 10.days).toInstant, Some(Map(Tier.Patron -> (eventDate - 40.days).toInstant)))

      ctaFor(dates, None) must be("join")
      ctaFor(dates, Some(Supporter)) must be("upgrade")
      ctaFor(dates, Some(Patron)) must be("buy")
    }

    "allow all members to buy if general release has arrived" in {
      val dates = TicketSaleDates((eventDate - 50.days).toInstant, Some(Map(Tier.Patron -> (eventDate - 60.days).toInstant)))

      ctaFor(dates, None) must be("join")
      ctaFor(dates, Some(Supporter)) must be("buy")
      ctaFor(dates, Some(Patron)) must be("buy")
    }
  }

}
