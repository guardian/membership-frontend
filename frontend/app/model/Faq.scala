package model

import configuration.{Config, Email}
import controllers.routes
import utils.StringUtils.slugify
import play.twirl.api.Html

object Faq {
  case class Section(title: String, questions:List[Item]) {
    def id = "s_" concat title.filter(_.isLetter)
  }
  case class Item(question: String, answer: Html) {
    def id = "q_" concat question.filter(_.isLetter)
  }

  val staff = List(
    Item("I already have a profile set up on theguardian.com, can I use that?",
       Html(s"Yes, but only if it's set up with your work email address ending @theguardian.com (@guardian.co.uk will work too). <a href='${Config.idWebAppUrl + "/account/edit"}'>Check my profile on the guardian.com</a>")
    ),
    Item("Can I use social sign in/registration?",
      Html("No, you must have a Guardian account using your work email address.")
    ),
    Item("I've already joined Guardian Members as a friend, can I upgrade?",
      Html("No, sorry you can't upgrade, you need to cancel your friend membership first.")
    ),
    Item("How do I cancel my friend membership?",
      Html(s"<ol><li>Go to <a href='${Config.membershipUrl + "/tier/cancel"}'>${Config.membershipUrl + "/tier/cancel"}</a></li><li>Scroll to the bottom and click 'Cancel membership'.</li></ol>")
    ),
    Item("How do I change the email address saved in my profile on theguardian.com?",
      Html(s"<ol><li>Go to <a href='${Config.idWebAppUrl + "/account/edit"}'>${Config.idWebAppUrl + "/account/edit"}</a></li><li>Change the email address to your work email address</li><li>Scroll to the bottom and click 'Save Changes'</li></ol>")
    ),
    Item("How long will staff membership last?",
      Html("Your Guardian Staff Membership will last for a long as you are a permanent member of staff")
    ),
    Item("I've got an additional question that's not listed here",
      Html(s"Please email <a href='mailto:${Email.staffMembership}'>${Email.staffMembership}</a> with your question, use Guardian Staff Partners as the Subject. We will do our best to get back to you within 24 hours.")
    )
  )

  val help = List(
    Section("Guardian Live (Events)",List(
      Item("What is Guardian Live?",Html("Guardian Live is a rolling events programme of discussions, debates, interviews and festivals that transform our journalism in print and online into live experiences. Recent examples include cooking lessons from Nigella Lawson and Jeanette Winterson, guided tours of the Guardian archives and printing presses, and talks from Johnny Marr, Bryan Cranston and Alan Bennett. Tickets can only be booked by Guardian Members.")),
      Item("Are all of your events in London?",Html("<p>We now run around 250 Guardian Live events a year in the UK. There are also a number of events held for members in the US and Australia. Around 35% of Guardian Live events happen outside London; that figure is not higher primarily because the additional costs involved in regional events (travel/accommodation/venue hire) can mean it’s hard to make the figures work. However, in addition, our regular Local programme takes place all over the country. Visit <a href='http://membership.theguardian.com/events'>membership.theguardian.com/events</a> to see the calendar of current events.</p><p>We plan to continue building our programme outside London and are working on a number of partnerships with venues and organisations. </p><p>We also have plans which will allow us to offer more to Members who can't make it to events. We try to live stream bigger events, our new monthly Members’ podcast will bring our lively newsroom events directly to Members and also feature their questions and viewpoints, and we continue to run live Q&As with our journalists on theguardian.com; on the day of the EU referendum result, editor-in-chief Katharine Viner and editor-at-large Gary Younge answered your questions.</p>")),
      Item("Can I buy a ticket to a Guardian Live event without becoming a Member?",Html("You can only buy a ticket to a Guardian Live event if you are a member. Access to these events is one of the benefits we offer Members in return for their support. ")),//TODO: should this refer to friend
      Item("Can I buy a ticket to an event without signing in?",Html("You are required to sign in before buying tickets for Guardian Live events.")),
      Item(" How many Guardian Live tickets can I buy for each event?",Html("You can buy as many tickets as you want. Partners are entitled to two tickets at a discount. After you’ve bought your discounted tickets you can come back and buy as many as you want at full price. We are working to improve this so you can buy all your tickets in one go.")),
      Item("If I choose the Tickets benefit, how do I redeem my tickets?",Html("If you have selected ‘tickets’ as your Partner benefit, your first six tickets to Guardian Live are available to book immediately. You may use one per event, and this will automatically be applied when you visit the booking page. Please note the six tickets are not valid for Guardian Local or Guardian Masterclasses.")),
      Item("If I choose the Book benefit, which books will I receive and when?",Html("If you have selected ‘books’ as your Partner benefit, you will receive four Guardian books specially selected for Members from Guardian just-published titles. You will receive your first book within 30 days of joining and one book every 3 months thereafter.")),
      Item("Can I change from the Tickets Benefit to the Book Benefit?",Html("Yes, you can choose to move from one benefit type to another, but only if you have not yet redeemed a ticket, or received a book in the applicable membership year. To do so, contact membership support by either emailing <a href='mailto:membershipsupport@theguardian.com'>membershipsupport@theguardian.com</a> or calling 0330 333 6898.")),
      Item("How will I know what Guardian Live events are coming up?",Html("Our events listing page is updated regularly and when you join as a Member you will receive a weekly email newsletter containing information about upcoming events.")),
      Item("How can I find out about disabled/wheelchair access for the venue?",Html("We only run events at venues that are accessible by wheelchair. You can tell us that you require accessibility information on the ticket booking form and we will contact you to go over the access route before the event."))
    )),
    Section("Guardian Masterclasses",List(
      Item("What are Guardian Masterclasses?",Html("Guardian Masterclasses match our readers’ interests with a wide range of courses and workshops, harnessing the expertise and specialisms of award-winning Guardian professionals and leading figures from the creative and digital industries. Recent examples include data visualisation workshops, a lyric-writing lesson from Chris Difford, and a masterclass in becoming a successful journalist from some of our award-winning writers. More frequently asked questions are available <a href='http://www.theguardian.com/guardian-masterclasses/faqs'>here</a>.")),
      Item("Can I book a Guardian Masterclass course without becoming a Member?",Html("Yes, Guardian Masterclasses are available to everyone from the Masterclasses website. However, only Partners can enjoy 20% off. In order to receive this discount Masterclasses must be booked through the Membership Masterclasses listings page.")),
      Item("How many Guardian Masterclasses places can I book for each course?",Html("You can book three tickets per transaction.")),
      Item("How will I know what Guardian Masterclasses events are coming up?",Html("Our Masterclasses listings page is updated regularly and when you join membership you will receive a weekly email newsletter containing information about upcoming courses.")),
      Item("Where do Guardian Masterclasses take place?",Html("Our Masterclasses are held in the meeting rooms and conference spaces of the Guardian's headquarters in King's Cross, London. View our maps to help you find your way to the Guardian offices, and find your way around inside. We also host Masterclasses in other venues in London and beyond.")),
      Item("What is the Guardian Masterclasses refund policy?",Html("You may cancel a Guardian Masterclass if your notification is received by the Masterclasses team a minimum of 14 days prior to the start of the Guardian Masterclass. Provided the Masterclasses team has received your notice of cancellation within this time frame (and acknowledged your notice), Masterclasses will refund any fees received from you less the deposit where applicable. Please ensure the Masterclasses team has acknowledged your notice of cancellation. We will not be able to refund you where we did not receive your notice 14 days before the start of the Guardian Masterclass (regardless of when you sent it). Our tickets are not transferable to other courses or dates. Find out how to contact the masterclass team here."))
    ))
  )
}
