package com.gu.zuora.soap.subscribe

import com.gu.zuora.soap.models.Commands._
import com.gu.zuora.soap.writers.XmlWriter
import com.gu.zuora.soap.writers.Command._
import org.specs2.mutable.Specification
import scalaz.Lens
import scalaz.syntax.std.option._
import scala.xml.Elem

class RatePlanTest extends Specification {
  "RatePlan" should {

    def charge(xml: Elem) = xml \ "RatePlanChargeData" \ "RatePlanCharge"

    def discount(period: Option[PeriodType], featureIds: Seq[String] = Nil) = {
      val edc = period.fold[EndDateCondition](SubscriptionEnd)(p => FixedPeriod(3, p))
      XmlWriter.write(RatePlan("123", Some(ChargeOverride(productRatePlanChargeId = "cid", discountPercentage = 20d.some, triggerDate =  None, endDateCondition = edc.some, billingPeriod = period)), featureIds)).value

    }

    "generate correct XML for a rate plan with only a prpid" in {
      (XmlWriter.write(RatePlan("123", None)).value \ "RatePlan" \ "ProductRatePlanId").text mustEqual "123"
    }

    "generate correct XML for a rate plan with a prpid and a discount percentage" in {
      (charge(discount(Months.some)) \ "DiscountPercentage").text mustEqual "20.0"
    }

    "generate correct XML for a rate plan with a billing period override" in {

      val chargeOverride = ChargeOverride(productRatePlanChargeId = "prpcid", billingPeriod = Years.some)
      val ratePlan = RatePlan("prpid", chargeOverride.some)

      /*
       * Scalaz experimenting corner! A lens is a thing that lets you create copies of
       * objects, changing a deeply nested value. So you define a function to update the value and a function
       * to read the value and then you don't have to do like a.copy(b = b.copy(c = c.copy(foo = bar))) all the time
       *
       * http://eed3si9n.com/learning-scalaz/Lens.html
       *
       * I am not going to use this in any production code
       * but its fun to try new things like this in a narrow scope
       */

      val bp = Lens.lensu[RatePlan, Option[PeriodType]] (
        // so this function does the annoying copying that we want to avoid repeating all over the place
        (a, value) => a.copy(chargeOverride = a.chargeOverride.map(_.copy(billingPeriod = value))),
        _.chargeOverride.flatMap(_.billingPeriod)
      )

      (XmlWriter.write(ratePlan).value \\ "BillingPeriod").text mustEqual "Annual"

      // see here because we did all the tedious copying in the lens its easy to test more cases
      (XmlWriter.write(bp.mod(_=>Quarters.some, ratePlan)).value \\ "BillingPeriod").text mustEqual "Quarter"
      (XmlWriter.write(bp.mod(_=>Months.some, ratePlan)).value \\ "BillingPeriod").text mustEqual "Month"
    }

    "generate correct XML for a discounted rateplan for 3 months" in {
      (charge(discount(Months.some)) \ "UpToPeriods").text mustEqual "3"
      (charge(discount(Months.some)) \ "UpToPeriodsType").text mustEqual "Month"
    }

    "Generate correct XML for a discounted rate plan with no fixed end date" in {
      (discount(None) \\ "EndDateCondition").text mustEqual "SubscriptionEnd"
      (discount(None) \\ "UpToPeriodsType").length mustEqual 0
      (discount(None) \\ "UpToPeriods").length mustEqual 0
    }

    "add in product feature IDs correctly" in {
      (discount(Months.some, Seq("F1"))
        \ "SubscriptionProductFeatureList"
        \ "SubscriptionProductFeature"
        \ "FeatureId").text mustEqual "F1"
    }
  }
}
