package model

import configuration.Config
import play.twirl.api.Html

object Faq {

  case class Faq(question: String, answer: Html)

  val help = List(
    Faq("What's a Guardian Identity account?",
      Html(s"When you <a href='${Config.idWebAppRegisterUrl(controllers.routes.Joiner.staff().url)}'>register on theguardian.com</a> you are creating a Guardian Identity account")
    ),
    Faq("I already have a Guardian Identity account, can I use that?",
      Html("Yes, but only if it's set up with your work email address ending @@theguardian.com (@@guardian.co.uk will work too) Check my Guardian identity set up")
    ),
    Faq("Can I use social sign in/registration?",
      Html("No, you must have a Guardian account using your work email address.")
    ),
    Faq("I've already joined Guardian Membership as a friend, can I upgrade?",
      Html("No, sorry you can't upgrade, you need to cancel your friend membership first.")
    ),
    Faq("How do I cancel my friend membership?",
      Html(s"<ol><li>Go to <a href='${Config.guardianMembershipUrl + "/tier/cancel"}'>${Config.guardianMembershipUrl + "/tier/cancel"}</a></li><li>Scroll to the bottom and click 'Cancel membership'.</li></ol>")
    ),
    Faq("How do I change my Guardian Identity email address?",
      Html(s"<ol><li>Go to <a href='${Config.idWebAppUrl + "/account/edit"}'>${Config.idWebAppUrl + "/account/edit"}</a></li><li>Change the email address to your work email address</li><li>Scroll to the bottom and click 'Save Changes'</li></ol>")
    ),
    Faq("How long will staff membership last?",
      Html("Your Guardian Staff Membership will last for a long as you are a permanent member of staff")
    ),
    Faq("I've got an additional question that's not listed here",
      Html(s"Please email <a href='mailto:${Config.membershipSupport}'>${Config.membershipSupport}</a> with your question, use Guardian Staff Partners as the Subject. We will do our best to get back to you within 24 hours.")
    )
  )

}
