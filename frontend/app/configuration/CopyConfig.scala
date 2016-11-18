package configuration

import com.typesafe.config.ConfigFactory

object CopyConfig {
  val config = ConfigFactory.load("copy.conf")

  val copyTitleDefault = config.getString("copy.default.title")
  val copyDescriptionDefault = config.getString("copy.default.description")

  val copyTitleJoin = config.getString("copy.join.title")
  val copyDescriptionJoin = config.getString("copy.join.description")

  val copyTitleChooseTier = config.getString("copy.choosetier.title")
  val copyDescriptionChooseTier = config.getString("copy.choosetier.description")

  val copyTitleSupporters = config.getString("copy.supporters.title")
  val copyDescriptionSupporters = config.getString("copy.supporters.description")

  val copyTitlePatrons = config.getString("copy.patrons.title")
  val copyDescriptionPatrons = config.getString("copy.patrons.description")

  val copyTitleEvents = config.getString("copy.events.title")
  val copyDescriptionEvents = config.getString("copy.events.description")

  val copyTitleMasterclasses = config.getString("copy.masterclasses.title")
  val copyDescriptionMasterclasses = config.getString("copy.masterclasses.description")

  val copyTitleAbout = config.getString("copy.about.title")
  val copyDescriptionAbout = config.getString("copy.about.description")

  // TODO: add to configuration
  val usLegalTerms = "NOTICE REGARDING AUTOMATIC RENEWAL: IF YOU DO NOT CANCEL YOUR MEMBERSHIP PRIOR TO THE END OF YOUR" +
    " INITIAL MEMBERSHIP PERIOD, YOUR MEMBERSHIP WILL AUTOMATICALLY RENEW FOR A RENEWAL MEMBERSHIP PERIOD OF THE SAME" +
    " LENGTH AS YOUR INITIAL MEMBERSHIP PERIOD, AND WE WILL CHARGE YOUR PAYMENT ACCOUNT THE APPLICABLE MEMBERSHIP FEE" +
    " ASSOCIATED WITH THE APPLICABLE MEMBERSHIP PERIOD.  YOU MAY CANCEL YOUR MEMBERSHIP AT ANY TIME PRIOR TO THE" +
    " COMMENCEMENT OF THE APPLICABLE RENEWAL MEMBERSHIP PERIOD AND YOUR MEMBERSHIP WILL NOT BE SUBJECT TO FURTHER RENEWAL​."

}
