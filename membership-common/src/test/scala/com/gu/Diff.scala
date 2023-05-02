package com.gu

import com.gu.memsub.subsv2.{PaidCharge, PaidChargeList, PaperCharges}
import com.gu.memsub.{Benefit, BillingPeriod}
import com.softwaremill.diffx.scalatest.DiffMatcher._
import com.softwaremill.diffx.{DiffContext, DiffResultValue, Diff => Diffx}
import org.scalatest.matchers.should.Matchers._
import scalaz.{Validation, \/}

object Diff {

  def assertEquals[T: Diffx](expected: T, actual: T) =
    actual should matchTo(expected)

  implicit def eitherDiff[L: Diffx, R: Diffx] = Diffx.derived[L \/ R]
  implicit def validationDiff[E: Diffx, A: Diffx] = Diffx.derived[Validation[E, A]]

  implicit val benefitDiff: Diffx[Benefit] = Diffx.diffForString.contramap(_.id)
  implicit def paidChargeListDiff[TheBenefit <: Benefit, TheBillingPeriod <: BillingPeriod](implicit
    e1: Diffx[PaperCharges],
    e2: Diffx[PaidCharge[TheBenefit, TheBillingPeriod]]
  ): Diffx[PaidChargeList] = (left: PaidChargeList, right: PaidChargeList, context: DiffContext) => (left, right) match {
    case (cl: PaperCharges, cr: PaperCharges) =>
      Diffx[PaperCharges].apply(cl, cr)
    case (cl: PaidCharge[TheBenefit, TheBillingPeriod], cr: PaidCharge[TheBenefit, TheBillingPeriod]) =>
      Diffx[PaidCharge[TheBenefit, TheBillingPeriod]].apply(cl, cr)
    case _ =>
      DiffResultValue(left, right)
  }

}
