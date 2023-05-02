package com.gu.memsub
import org.specs2.mutable.Specification
import com.gu.memsub.BillingSchedule.{Bill, BillItem, _}
import com.gu.zuora.soap.models.Queries._
import com.github.nscala_time.time.Imports._

import scalaz.{IList, NonEmptyList}
import scalaz.syntax.nel._
import org.joda.time.LocalDate
import com.gu.memsub.Subscription.ProductRatePlanChargeId
import com.gu.lib.DateDSL._
import com.gu.memsub.Benefit.Digipack

class BillingScheduleTest extends Specification {

  "getBillingSchedule" should {

    val jan15 = new LocalDate("2015-01-01")
    val feb15 = new LocalDate("2015-02-01")
    val mar15 = new LocalDate("2015-03-01")
    val apr15 = new LocalDate("2015-04-01")
    val may15 = new LocalDate("2015-05-01")
    val jun15 = new LocalDate("2015-06-01")

    "Stick together invoice items with the same ServiceStartDate" in {
      val invoiceItems = Seq(
        PreviewInvoiceItem(15.0f, jan15, feb15, "normal product id", "", "Digipack", 15.0f),
        PreviewInvoiceItem(-3.75f, jan15, feb15, "discount product id", "", "Discount", 30.0f)
      )

      val expectedSchedule = BillingSchedule(NonEmptyList(Bill(jan15, 1.month, NonEmptyList(
        BillItem("Digipack", None, 15.0f, 15.0f),
        BillItem("Discount", None, -3.75f, 30.0f))
      )))
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems) mustEqual Some(expectedSchedule)
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems).get.first.amount mustEqual 11.25f
    }

    "Not stick together invoice items with different ServiceEndDates" in {
      val invoiceItems = Seq(
        PreviewInvoiceItem(15.0f, jan15, feb15, "normal product id", "", "Charge name", 15.0f),
        PreviewInvoiceItem(15.0f, feb15, mar15, "some other product id", "", "Charge name", 15.0f)
      )

      val expectedSchedule = BillingSchedule(NonEmptyList(
        Bill(jan15, 1.month, BillItem("Charge name", None, 15f, 15f).wrapNel),
        Bill(feb15, 1.month, BillItem("Charge name", None, 15f, 15f).wrapNel)
      ))
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems) mustEqual Some(expectedSchedule)
    }

    "Merge credits occurring half way through a month into the subsequent bill" in {
      val start = new LocalDate("2016-01-01")

      val withHolidays = Seq(
        PreviewInvoiceItem(-15.0f, start.plusDays(14), start.plusDays(15), "A holiday discount", "", "Discount", -15.0f),
        PreviewInvoiceItem(15.0f, start.plusMonths(1), start.plusMonths(2), "some other product id", "", "Normal bill", 15.0f),
        PreviewInvoiceItem(15.0f, start.plusMonths(2), start.plusMonths(3), "some other product id", "", "Also normal", 15.0f),
        PreviewInvoiceItem(-1.0f, start.plusMonths(2).plusDays(1), start.plusMonths(2).plusDays(2), "", "some other product id", "Another discount", -1.0f),
        PreviewInvoiceItem(15.0f, start.plusMonths(3), start.plusMonths(4), "some other product id", "", "Another normal charge", 15.0f)
      )

      BillingSchedule.fromPreviewInvoiceItems(_ => None)(withHolidays) mustEqual Some(BillingSchedule(NonEmptyList(
        Bill(start.plusMonths(1), 1.month, NonEmptyList(BillItem("Normal bill", None, 15f, 15f), BillItem("Discount", None, -15f, -15f))),
        Bill(start.plusMonths(2), 1.month, NonEmptyList(BillItem("Also normal", None, 15f, 15f))),
        Bill(start.plusMonths(3), 1.month, NonEmptyList(BillItem("Another normal charge", None, 15f, 15f), BillItem("Another discount", None, -1f, -1f)))
      )))
    }

    "Handle floating point math which may sum things greater than 2dp, e.g. 20.74 => 20.740002" in {
      val invoiceItems = Seq(
        PreviewInvoiceItem(20.74f, jan15, feb15, "migration adjustment", "", "Adjustment charge", -58.46f),
        PreviewInvoiceItem(-2.7f, jan15, feb15, "hd package product id", "", "Monday - proration credit", 7.61f),
        PreviewInvoiceItem(-2.7f, jan15, feb15, "hd package product id", "", "Thursday - proration credit", 7.61f),
        PreviewInvoiceItem(-2.7f, jan15, feb15, "hd package product id", "", "Friday - proration credit", 7.61f),
        PreviewInvoiceItem(-2.7f, jan15, feb15, "hd package product id", "", "Tuesday - proration credit", 7.61f),
        PreviewInvoiceItem(-3.67f, jan15, feb15, "hd package product id", "", "Sunday - proration credit", 10.34f),
        PreviewInvoiceItem(-3.57f, jan15, feb15, "hd package product id", "", "Saturday - proration credit", 10.07f),
        PreviewInvoiceItem(-2.7f, jan15, feb15, "hd package product id", "", "Wednesday - proration credit", 7.61f)
      )

      val expectedSchedule = BillingSchedule(NonEmptyList(Bill(jan15, 1.month, NonEmptyList(
        BillItem("Adjustment charge", None, 20.74f, -58.46f),
        BillItem("Monday - proration credit", None, -2.7f, 7.61f),
        BillItem("Thursday - proration credit", None, -2.7f, 7.61f),
        BillItem("Friday - proration credit", None, -2.7f, 7.61f),
        BillItem("Tuesday - proration credit", None, -2.7f, 7.61f),
        BillItem("Sunday - proration credit", None, -3.67f, 10.34f),
        BillItem("Saturday - proration credit", None, -3.57f, 10.07f),
        BillItem("Wednesday - proration credit", None, -2.7f, 7.61f)
      ))))

      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems) mustEqual Some(expectedSchedule)
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems).get.first.totalGross mustEqual 20.74f
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems).get.first.totalDeductions mustEqual 20.74f
      BillingSchedule.fromPreviewInvoiceItems(_ => None)(invoiceItems).get.first.amount mustEqual 0f
    }

    "Zero out negative invoices with a non zero gross and add the remainder to the following invoice" in {
      val start = new LocalDate("2016-01-01")

      val withHolidays = Seq(
        PreviewInvoiceItem(-18.0f, start, start.plusMonths(1), "Some kind of deduction", "", "Discount", -18.0f),
        PreviewInvoiceItem(15.0f, start, start.plusMonths(1), "some other product id", "", "Normal bill", 15.0f),
        PreviewInvoiceItem(15.0f, start.plusMonths(1), start.plusMonths(2), "some other product id", "", "Normal", 15.0f)
      )

      BillingSchedule.fromPreviewInvoiceItems(_ => None)(withHolidays) mustEqual Some(BillingSchedule(NonEmptyList(
        Bill(start, 1.month, NonEmptyList(BillItem("Credit balance", None, 3, 3), BillItem("Discount", None, -18f, -18f), BillItem("Normal bill", None, 15f, 15f))),
        Bill(start.plusMonths(1), 1.month, NonEmptyList(BillItem(s"Credit from $start", None, -3, -3), BillItem("Normal", None, 15f, 15f)))
      )))
    }

    "Daisy chain large credits over more than one month" in {

      val withHolidays = Seq(
        PreviewInvoiceItem(-40.0f, 15 Jun 2016, 16 Jun 2016, "", "", "Holidays!", -40.0f),
        PreviewInvoiceItem(15.0f, 1 Jul 2016, 1 Aug 2016, "", "", "Everyday+", 15.0f),
        PreviewInvoiceItem(15.0f, 1 Aug 2016, 1 Sep 2016, "", "", "Everyday+", 15.0f),
        PreviewInvoiceItem(15.0f, 1 Sep 2016, 1 Oct 2016, "", "", "Everyday+", 15.0f)
      )

      BillingSchedule.fromPreviewInvoiceItems(_ => None)(withHolidays) mustEqual Some(BillingSchedule(NonEmptyList(
        Bill(1 Jul 2016, 1.month, NonEmptyList(BillItem("Credit balance", None, 25, 25), BillItem("Everyday+", None, 15.0f, 15.0f), BillItem("Holidays!", None, -40, -40))),
        Bill(1 Aug 2016, 1.month, NonEmptyList(BillItem("Credit balance", None, 10, 10), BillItem("Credit from 2016-07-01", None, -25, -25), BillItem("Everyday+", None, 15.0f, 15.0f))),
        Bill(1 Sep 2016, 1.month, NonEmptyList(BillItem("Credit from 2016-08-01", None, -10, -10), BillItem("Everyday+", None, 15.0f, 15.0f)))
      )))
    }

    "Include any products found with the productFinder in the bill items" in {
      val start = new LocalDate("2016-01-01")

      val discerningFinder: ProductRatePlanChargeId => Option[Benefit] = {
        case ProductRatePlanChargeId("DigiPrpcId") => Some(Digipack)
        case _ => None
      }

      val items = Seq(
        PreviewInvoiceItem(15.0f, start, start.plusMonths(1), "A holiday discount", "DigiPrpcId", "Month 1", 15.0f),
        PreviewInvoiceItem(15.0f, start.plusMonths(1), start.plusMonths(2), "some other product id", "Foo", "Month 2", 15.0f)
      )

      BillingSchedule.fromPreviewInvoiceItems(discerningFinder)(items) mustEqual Some(BillingSchedule(NonEmptyList(
        Bill(start, 1.month, NonEmptyList(BillItem("Month 1", Some(Digipack), 15f, 15f))),
        Bill(start.plusMonths(1), 1.month, NonEmptyList(BillItem("Month 2", None, 15f, 15f)))
      )))
    }

    "Roll up to a shorter view" in {
      val thereafterBill = Bill(7 Mar 2016, 1.month, NonEmptyList(BillItem("Some package", None, 15.0f, 15.0f)))
      val trimmedSchedule = IList[Bill](
        Bill(7 Jan 2016, 1.month, NonEmptyList(BillItem("Some package", None, 15.0f, 15.0f))),
        Bill(7 Feb 2016, 1.month, NonEmptyList(BillItem("Some package", None, 12.0f, 12.0f)))
      )

      val schedule1 = BillingSchedule(NonEmptyList(thereafterBill))

      val schedule2 = BillingSchedule(trimmedSchedule <::: NonEmptyList(thereafterBill))

      val schedule3 = BillingSchedule(trimmedSchedule :+ thereafterBill <::: NonEmptyList(
        Bill(7 Apr 2016, 1.month, NonEmptyList(BillItem("Some package", None, 15.0f, 15.0f))),
        Bill(7 May 2016, 1.month, NonEmptyList(BillItem("Some package", None, 15.0f, 15.0f)))
      ))

      rolledUp(schedule1) mustEqual(thereafterBill, List.empty)
      rolledUp(schedule2) mustEqual(thereafterBill, trimmedSchedule.toList)
      rolledUp(schedule3) mustEqual(thereafterBill, trimmedSchedule.toList)
    }

    "Applies an account credit to the first positive invoice" in {
      val confusingQuarterlyEchoLegacySchedule = BillingSchedule(NonEmptyList(
        Bill(jan15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(feb15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(mar15, 1.month, NonEmptyList(
          BillItem("Monday", None, 0, 0),
          BillItem("Tuesday", None, 15f, 15f),
          BillItem("Wednesday", None, 15f, 15f),
          BillItem("Thursday", None, 15f, 15f),
          BillItem("Friday", None, 15f, 15f),
          BillItem("Saturday", None, 15f, 15f),
          BillItem("Sunday", None, 15f, 15f)
        )),
        Bill(apr15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(may15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(jun15, 1.month, NonEmptyList(
          BillItem("Monday", None, 0, 0),
          BillItem("Tuesday", None, 15f, 15f),
          BillItem("Wednesday", None, 15f, 15f),
          BillItem("Thursday", None, 15f, 15f),
          BillItem("Friday", None, 15f, 15f),
          BillItem("Saturday", None, 15f, 15f),
          BillItem("Sunday", None, 15f, 15f)
        ))
      ))
      val scheduleWithCreditDisplayed = confusingQuarterlyEchoLegacySchedule.withCreditBalanceApplied(2.1f)
      val displayedInvoices = scheduleWithCreditDisplayed.invoices.list
      displayedInvoices.toList(0).amount mustEqual 0f
      displayedInvoices.toList(1).amount mustEqual 0f
      displayedInvoices.toList(2).amount mustEqual 90f - 2.1f
      displayedInvoices.toList(3).amount mustEqual 0f
      displayedInvoices.toList(4).amount mustEqual 0f
      displayedInvoices.toList(5).amount mustEqual 90f
    }

    "Spread a large account credit over the available positive invoices" in {
      val confusingQuarterlyEchoLegacySchedule = BillingSchedule(NonEmptyList(
        Bill(jan15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(feb15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(mar15, 1.month, NonEmptyList(
          BillItem("Monday", None, 0, 0),
          BillItem("Tuesday", None, 15f, 15f),
          BillItem("Wednesday", None, 15f, 15f),
          BillItem("Thursday", None, 15f, 15f),
          BillItem("Friday", None, 15f, 15f),
          BillItem("Saturday", None, 15f, 15f),
          BillItem("Sunday", None, 15f, 15f)
        )),
        Bill(apr15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(may15, 1.month, BillItem("Monday", None, 0, 0).wrapNel),
        Bill(jun15, 1.month, NonEmptyList(
          BillItem("Monday", None, 0, 0),
          BillItem("Tuesday", None, 15f, 15f),
          BillItem("Wednesday", None, 15f, 15f),
          BillItem("Thursday", None, 15f, 15f),
          BillItem("Friday", None, 15f, 15f),
          BillItem("Saturday", None, 15f, 15f),
          BillItem("Sunday", None, 15f, 15f)
        ))
      ))
      val scheduleWithCreditDisplayed = confusingQuarterlyEchoLegacySchedule.withCreditBalanceApplied(100)
      val displayedInvoices = scheduleWithCreditDisplayed.invoices.list
      displayedInvoices.toList(0).amount mustEqual 0f
      displayedInvoices.toList(1).amount mustEqual 0f
      displayedInvoices.toList(2).amount mustEqual 0f
      displayedInvoices.toList(3).amount mustEqual 0f
      displayedInvoices.toList(4).amount mustEqual 0f
      displayedInvoices.toList(5).amount mustEqual 80f
    }
  }
}
