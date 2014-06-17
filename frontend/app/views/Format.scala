package views

import play.api.templates.Html

object Format {
  def formatPounds(pounds: Int): Html = {
    Html("&#163;" + pounds + "." + "00")
  }
  def formatPence(pence: Int): Html = {
    Html("&#163;" + (pence / 100) + "." + ("%02d".format(pence % 100)))
  }
}
