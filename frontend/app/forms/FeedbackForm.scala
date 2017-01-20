package forms

import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Mapping}
import model.FeedbackType

/**
  * Created by alex_ware on 20/01/2017.
  */
object FeedbackForm {
  case class FeedbackForm(category: FeedbackType, page: String, feedback: String, name: String, email: String)

  implicit val feedbackTypeFormatter: Formatter[FeedbackType] = new Formatter[FeedbackType] {override def unbind(key: String, value: FeedbackType) = Map(key -> value.slug)

    override def bind(key: String, data: Map[String, String]):Either[Seq[FormError],FeedbackType] = {
      val feedbackType = data.get(key)
      lazy val formError = FormError(key, s"Cannot find a feedback type of ${feedbackType.getOrElse("")}")
      feedbackType
        .flatMap(FeedbackType.fromSlug)
        .toRight[Seq[FormError]](Seq(formError))
    }
  }
  private val feedbackType = of[FeedbackType] as feedbackTypeFormatter


  val feedbackForm: Form[FeedbackForm] = Form(
    mapping(
      "category" -> feedbackType,
      "page" -> text,
      "feedback" -> nonEmptyText,
      "name" -> nonEmptyText,
      "email" -> email
    )(FeedbackForm.apply)(FeedbackForm.unapply)
  )
}
