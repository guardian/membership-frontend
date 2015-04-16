package model

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class MembershipSummaryTest extends Specification  {

  "MembershipSummary" should {
    val termStartDate = DateTime.now

    "have an annual setting of true for annual subscriptions when payment made at start of plan" in {
      val firstPaymentEndDate = termStartDate.plusYears(1).minusDays(1)
      val nextPaymentDate = firstPaymentEndDate.plusDays(1)
      val partnerAnnualSummary = MembershipSummary(termStartDate, firstPaymentEndDate, Some(135f), 135f, 135f, nextPaymentDate, nextPaymentDate)
      partnerAnnualSummary.annual mustEqual  true
    }

    "have an annual setting of false for monthly subscriptions when payment made at start of plan" in {
      val firstPaymentEndDate = termStartDate.plusMonths(1).minusDays(1)
      val nextPaymentDate = firstPaymentEndDate.plusDays(1)
      val partnerMonthlySummary = MembershipSummary(termStartDate, firstPaymentEndDate, Some(15f), 15f, 15f, nextPaymentDate, nextPaymentDate)
      partnerMonthlySummary.annual mustEqual  false
    }

    "have an annual setting of true for annual subscriptions with free period offer" in {
      val initialDelay = 6
      val firstPaymentStartDate = termStartDate.plusMonths(initialDelay)
      val firstPaymentEndDate = firstPaymentStartDate.plusMonths(12 - initialDelay).minusDays(1)
      val renewalDate = termStartDate.plusYears(1)

      val partnerAnnualSummary = MembershipSummary(termStartDate, firstPaymentEndDate, None, 135f, 67.5f, firstPaymentStartDate, renewalDate)
      partnerAnnualSummary.annual mustEqual  true
    }

    "have an annual setting of false for monthly subscriptions with free period offer" in {
      val initalDelayMonths = 6
      val firstPaymentStartDate = termStartDate.plusMonths(initalDelayMonths)
      val firstPaymentEndDate = termStartDate.plusMonths(1).minusDays(1)


      val partnerMonthlySummary = MembershipSummary(termStartDate, firstPaymentEndDate, None, 15f, 15f, firstPaymentStartDate, firstPaymentStartDate)
      partnerMonthlySummary.annual mustEqual  false
    }
  }

}