package com.gu.memsub

import java.lang.Math.min

import com.github.nscala_time.time.Imports._
import com.gu.memsub.BillingSchedule.Bill
import com.gu.memsub.Subscription.ProductRatePlanChargeId
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import org.joda.time.Period

import scala.annotation.tailrec
import scala.collection.immutable.Stream.Empty
import scala.math.BigDecimal.RoundingMode.{HALF_DOWN, HALF_UP}
import scala.math.BigDecimal._
import scalaz.syntax.std.list._
import scalaz.{NonEmptyList, Zipper}

/**
  * The high-level version of preview invoices
  * This is to a Zuora BillingPreview what Subscription is to a Zuora SOAP/REST sub
  */
case class BillingSchedule(invoices: NonEmptyList[Bill]) {
  def first: Bill = invoices.head

  def withCreditBalanceApplied(creditBalance: Float): BillingSchedule = {
    case class Cursor(remaingCredit: Float = creditBalance, invoicesSoFar: List[Bill] = List.empty) {
      def toNel = NonEmptyList.fromSeq(invoicesSoFar.head, invoicesSoFar.tail)
    }
    val processedInvoices = invoices.list.foldLeft(Cursor()) {
      case (cursor, nextInvoice) =>
        val creditToTransfer = min(nextInvoice.amount, cursor.remaingCredit)
        val newRemainder = cursor.remaingCredit - creditToTransfer
        val creditedInvoice = nextInvoice.receiveCredit(creditToTransfer)
        Cursor(newRemainder, cursor.invoicesSoFar :+ creditedInvoice)
    }
    BillingSchedule(processedInvoices.toNel)
  }
}

object BillingSchedule {

  type ProductFinder = ProductRatePlanChargeId => Option[Benefit]
  case class BillItem(name: String, product: Option[Benefit], amount: Float, unitPrice: Float) {
    override lazy val toString = s"| $amount\t| ($unitPrice)\t| $name | ${product.mkString}"
  }

  object BillItem {
    def fromItem(finder: ProductFinder)(in: PreviewInvoiceItem) =
      BillItem(in.chargeName, finder(ProductRatePlanChargeId(in.productRatePlanChargeId)), in.price, in.unitPrice)
  }

  case class Bill(date: LocalDate, duration: Period, items: NonEmptyList[BillItem], accountCredit: Option[Float] = None) {
    // Rounds amounts in a way which errs on telling the customer they may need to pay a few pence more than what Zuora may actually charge
    private val charges = items.list.filter(_.amount > 0f)
    private val deductions = items.list.filter(_.amount < 0f).map(_.amount).toList ++ accountCredit.filter(_ > 0f).map(_ * -1)
    val totalGross: Float = charges.map(_.amount.setScale(2, HALF_UP)).toList.sum.toFloat // amount excluding deductions
    val totalDeductions: Float = deductions.map(-_.setScale(2, HALF_DOWN)).sum.toFloat  // amount deducted
    val amount: Float = totalGross - totalDeductions
    def addItem(item: BillItem): Bill = copy(items = items.<::(item))
    override lazy val toString = s"\n$date:\n${items.list.toList.mkString("\n")}\n${accountCredit.map(c => s"Credit:\t-$c\n").mkString}Total:\t$amount\n"
    def receiveCredit(amount: Float): Bill = if (amount > 0) this.copy(accountCredit = Some(amount)) else this
  }

  object Bill {
    def fromGroupedItems(finder: ProductFinder)(date: LocalDate, items: NonEmptyList[PreviewInvoiceItem]) =
      Bill(date, new Period(date, items.map(_.serviceEndDate).list.toList.max), items.map(BillItem.fromItem(finder)))
  }

  def fromPreviewInvoiceItems(finder: ProductFinder)(invoices: Seq[PreviewInvoiceItem]): Option[BillingSchedule] = {

    @tailrec
    def sortOutCredits(z: Zipper[Bill]): NonEmptyList[Bill] = z match {

      // the end of the list
      case Zipper(l, normal, Empty) =>
        (l.toList :+ normal).toNel.toOption.get

      // we have a negative bill, so we want to zero it and credit the next bill
      case Zipper(l, bill, r #:: rs) if bill.totalGross > 0 && bill.amount < 0 =>
        val creditAmount = bill.totalDeductions - bill.totalGross
        val debitAmount = bill.totalGross - bill.totalDeductions
        val cbCredit = BillItem("Credit balance", None, creditAmount, creditAmount)
        val cbDebit = BillItem(s"Credit from ${bill.date}", None, debitAmount, debitAmount)
        sortOutCredits(Zipper(l.append(Stream(bill.addItem(cbCredit))), r.addItem(cbDebit), rs))

      // we have a credit note which we want to remove and assign to the next bill
      case Zipper(l, credit, r #:: rs) if credit.totalGross == 0 && credit.amount < 0 =>
        sortOutCredits(Zipper(l, r.copy(items = r.items.append(credit.items)), rs))

      // we have a normal invoice
      case Zipper(l, f, r #:: rs) =>
        sortOutCredits(Zipper(l :+ f, r, rs))
    }

    invoices.toList
      .groupBy1(_.serviceStartDate).toList
      .map((Bill.fromGroupedItems(finder) _).tupled)
      .sortBy(_.date).toNel.toOption.map(invs => BillingSchedule(sortOutCredits(invs.toZipper)))
  }

  def rolledUp(in: BillingSchedule): (Bill, Seq[Bill]) = {
    val reversedSchedule = in.invoices.reverse
    val thereafterBill = reversedSchedule.list.takeWhile(_.amount == reversedSchedule.head.amount).lastOption.getOrElse(reversedSchedule.head)
    val trimmedSchedule = reversedSchedule.list.dropWhile(_.amount == thereafterBill.amount).reverse.toList
    (thereafterBill, trimmedSchedule)
  }
}
