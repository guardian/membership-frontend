package views

object Format {
  def formatPounds(pounds: Int) = {
    "&#163;" + pounds + "." + "00"
  }
  def formatPence(pence: Int) = {
    "&#163;" + (pence / 100) + "." + ("%02d".format(pence % 100))
  }
}
