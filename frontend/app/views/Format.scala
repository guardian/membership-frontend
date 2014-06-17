package views

object Format {
  def formatPounds(pounds: Int): String = {
    "£" + pounds + "." + "00"
  }
  def formatPence(pence: Int): String = {
    "£" + (pence / 100) + "." + ("%02d".format(pence % 100))
  }
}
