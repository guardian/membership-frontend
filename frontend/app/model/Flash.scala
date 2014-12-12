package model

object Flash {

  trait Message {
    val textOpt: Option[String]
  }

  case class ErrorMessage(textOpt: Option[String]) extends Message
  case class SuccessMessage(textOpt: Option[String]) extends Message
}
