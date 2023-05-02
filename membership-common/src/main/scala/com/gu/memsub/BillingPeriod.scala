package com.gu.memsub

sealed trait BillingPeriod {
  def noun: String
  def adverb: String
  def adjective: String
  def annual: Boolean = false
  def monthsInPeriod: Int
}

object BillingPeriod {

  sealed trait RecurringPeriod extends BillingPeriod

  sealed trait OneOffPeriod extends BillingPeriod

  case object Month extends RecurringPeriod {
    override def noun = "month"
    override def adverb = "monthly"
    override def adjective = "monthly"
    override def monthsInPeriod = 1
  }

  case object Quarter extends RecurringPeriod {
    override def noun = "quarter"
    override def adverb = "quarterly"
    override def adjective = "quarterly"
    override def monthsInPeriod = 3
  }

  case object SixMonthsRecurring extends RecurringPeriod {
    override def noun = "six months"
    override def adverb = "six monthly"
    override def adjective = "six monthly"
    override def monthsInPeriod = 6
  }

  case object Year extends RecurringPeriod {
    override def noun = "year"
    override def adverb = "annually"
    override def adjective = "annual"
    override def annual = true
    override def monthsInPeriod = 12
  }

  case object OneYear extends OneOffPeriod {
    override def noun = "1 year"
    override def adverb = "1 year"
    override def adjective = "1 year"
    override def monthsInPeriod = 12
  }
  case object TwoYears extends OneOffPeriod {
    override def noun = "2 years"
    override def adverb = "2 years"
    override def adjective = "2 years"
    override def monthsInPeriod = 24
  }
  case object ThreeYears extends OneOffPeriod {
    override def noun = "3 years"
    override def adverb = "3 years"
    override def adjective = "3 years"
    override def monthsInPeriod = 36
  }
  case object SixWeeks extends OneOffPeriod{
    override def noun = "6 weeks"
    override def adverb = "6 weeks"
    override def adjective = "6 weeks"
    override def monthsInPeriod = 1 // todo this doesn't make sense for periods that are not a exact number of months
  }
  case object SixMonths extends OneOffPeriod{
    override def noun = "6 months"
    override def adverb = "6 months"
    override def adjective = "6 months"
    override def monthsInPeriod = 6
  }
  case object ThreeMonths extends OneOffPeriod{
    override def noun = "3 months"
    override def adverb = "3 months"
    override def adjective = "3 months"
    override def monthsInPeriod = 3
  }
  case object OneTimeChargeBillingPeriod extends OneOffPeriod{
    override def noun = "One time charge"
    override def adverb = "One time charge"
    override def adjective = "One time charge"
    override def monthsInPeriod = 1 // this doesn't really make sense for a one time charge
  }
}
