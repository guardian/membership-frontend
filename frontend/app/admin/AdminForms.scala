package admin

import play.api.data.Forms._
import play.api.data.Form

object AdminForms {

  case class EditForm(
    ticketingProvider: String,
    gridUrl: Option[String]
  )

  val editForm: Form[EditForm] = Form(
    mapping(
      "ticketingProvider" -> nonEmptyText,
      "gridUrl" -> optional(text)
    )(EditForm.apply)(EditForm.unapply)
  )

}
