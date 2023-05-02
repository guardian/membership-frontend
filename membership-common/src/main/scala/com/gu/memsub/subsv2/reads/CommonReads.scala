package com.gu.memsub.subsv2.reads

import com.gu.memsub._
import play.api.libs.json.{Reads, _}

import scalaz.std.list._
import scalaz.std.option._
import scalaz.syntax.std.list._
import scalaz.syntax.traverse._
import scalaz.{Applicative, Failure, NonEmptyList, Success, ValidationNel}

object CommonReads {

  val dateFormat = "yyyy-MM-dd"
  implicit val dateTimeReads = JodaReads.jodaDateReads(dateFormat)
  implicit val dateTimeWrites = JodaWrites.jodaDateWrites(dateFormat)
  implicit val localReads = JodaReads.jodaLocalDateReads(dateFormat)
  implicit val localWrites = JodaWrites.jodaLocalDateWrites(dateFormat)

  implicit class JsResultOption[A](opt: Option[A]) {
    def toJsSuccess(error: String) = opt match {
      case Some(a) => JsSuccess(a)
      case _ => JsError(error)
    }
  }

  // since we don't have a stack to trace, we need to make our own
  implicit class TraceableValidation[T](t: ValidationNel[String, T]) {
    def withTrace(message: String): ValidationNel[String, T] = t match {
      case Failure(e: NonEmptyList[String]) => Failure(e.map(error => s"$message: $error"))
      case right => right
    }
  }

  /**
    * play provides its own applicative instance in play.api.libs.functional.syntax
    * where you use "and" instead of |@| but that is FAR TOO READABLE TO BE TRUSTED.
    * more seriously you need an applicative instance to convert a List[JsResult[A]] to a JsResult[List[A]]
    * and so we might as well be consistent and use it over play's attempts
    */
  implicit object JsResultApplicative extends Applicative[JsResult] {
    override def point[A](a: => A): JsResult[A] = JsSuccess(a)

    override def ap[A, B](fa: => JsResult[A])(f: => JsResult[(A) => B]): JsResult[B] = (fa, f) match {
      case (JsSuccess(a, _), JsSuccess(func, _)) => JsSuccess(func(a))
      case (err1@JsError(_), err2@JsError(_)) => err1 ++ err2
      case (err@JsError(_), _) => err
      case (_, err@JsError(_)) => err
    }
  }

  implicit class FailureAggregatingOrElse[X](validationNel: ValidationNel[String, X]) {
    def orElse2[A >: X, Y <: A](fallback: ValidationNel[String, Y]): ValidationNel[String, A] = {
      validationNel match {
        case Failure(firstFailures) => fallback.leftMap(moreFailures => NonEmptyList.fromSeq[String](firstFailures.head, firstFailures.tail.toList ++ moreFailures.list.toList)).map(identity[A])
        case Success(_) => validationNel.map(identity[A])
      }
    }
  }

  implicit val pricingSummaryReads = new Reads[PricingSummary] {
    override def reads(json: JsValue): JsResult[PricingSummary] = {

      // for subscriptions our pricing summary is a string i.e. 10GBP, for the catalog its an array
      val normalisedPricingList = json.validate[List[String]] orElse json.validate[String].map(List(_))

      val parsedPrices = normalisedPricingList.flatMap { priceStrings =>
        priceStrings.map(PriceParser.parse)
          .sequence[Option, Price]
          .toJsSuccess(s"Failed to parse $normalisedPricingList")
      }

      parsedPrices.map(priceList =>
        priceList.map(p => p.currency -> p).toMap
      ).map(PricingSummary(_))
    }
  }

  // this reader reads a list but only throws an error if everything failed
  implicit def niceListReads[A: Reads]: Reads[List[A]] = new Reads[List[A]] {
    override def reads(json: JsValue): JsResult[List[A]] = json match {
      case JsArray(items) =>
        items.map(_.validate[A]).partition(_.isSuccess) match {
          case (successes, errors) if successes.nonEmpty || errors.isEmpty =>
            (successes.toList: List[JsResult[A]]).sequence[JsResult, A]
          case (successes, errors) if successes.isEmpty => JsError(errors.mkString)
        }
      case _ => JsError(s"Failed to read $json as a list")
    }
  }

  implicit def nelReads[A](implicit r: Reads[List[A]]): Reads[NonEmptyList[A]] =
    (json: JsValue) => {
      val jsResult = r.reads(json)
      val errors = jsResult.asEither.left.toOption.mkString
      jsResult.flatMap(_.toNel.toOption.fold[JsResult[NonEmptyList[A]]](JsError(s"List was empty - $errors"))(JsSuccess(_)))
    }
}
