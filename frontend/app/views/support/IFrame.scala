package views.support

object IFrame {
  def calcEBIFrameHeight(amount: Int): Int = {
    lazy val ebIFrameChrome = 176
    lazy val ebTicketTrHeight = 60

    amount * ebTicketTrHeight + ebIFrameChrome
  }
}
