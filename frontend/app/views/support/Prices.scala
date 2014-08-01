package views.support

object Prices {
  implicit class RichFloat(price: Float) {
    lazy val pretty = "£%.2f".format(price)
  }
}
