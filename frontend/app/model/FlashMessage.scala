package model

abstract case class FlashMessage(level: String, message: String)

object FlashMessage {
  def error(message: String) = new FlashMessage("error", message) {}
  def success(message: String) = new FlashMessage("success", message) {}
}
