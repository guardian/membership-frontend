package model

import configuration.Config
import play.twirl.api.Html

object Faq {

  case class Item(question: String, answer: Html, id: String = "")

  val staff = List(
    Item("I already have a profile set up on theguardian.com, can I use that?",
       Html(s"Yes, but only if it's set up with your work email address ending @theguardian.com (@guardian.co.uk will work too). <a href='${Config.idWebAppUrl + "/account/edit"}'>Check my profile on the guardian.com</a>")
    ),
    Item("Can I use social sign in/registration?",
      Html("No, you must have a Guardian account using your work email address.")
    ),
    Item("I've already joined Guardian Membership as a friend, can I upgrade?",
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
      Html(s"Please email <a href='mailto:${Config.membershipSupportStaffEmail}'>${Config.membershipSupportStaffEmail}</a> with your question, use Guardian Staff Partners as the Subject. We will do our best to get back to you within 24 hours.")
    )
  )

  val help = List(
    Item("What is Guardian Membership?",
      Html("Guardian Membership is for you if you share our belief that the open exchange of information, ideas and opinions can change the world for the better. By signing up and paying your subscription (at Partner and Patron level), you are helping bring these words to life by supporting open and independent journalism. In return, you get access to events and experiences that are only available to members."),
      "what-is-membership"
    ),
    Item("Why should I join?",
      Html("Membership has a lot to offer, and by joining in this beta phase, you can help build and shape Guardian Membership into one of the world’s largest communities of free thinkers. Benefits include access to the Guardian live events programme. Events range from lectures by the world’s foremost thinkers to exclusive screenings of the latest independent films; from sessions with senior journalists at our offices in Kings Place to more intimate, bespoke events. Tickets are only available to Guardian members. If you want to support the Guardian and its journalism, and keep our website open to all, then joining Guardian Membership and paying your subscription is a great way to do it."),
      "why-join"
    ),
    Item("How much does membership cost?",
      Html("The Friend tier of Guardian Membership is free. Partners pay £15 per month or £135 per year if you pay in one go. For Patrons, the cost is £60 per month or £540 per year. If you choose the annual payment you get 3 months free."),
      "how-much-does-it-cost"
    ),
    Item("What is the difference between a Friend, a Partner and a Patron?",
      Html("You can read a full description of each membership tier <a href='join'>here</a>."),
      "difference-between-tiers"
    ),
    Item("Why do I have to sign in to the Guardian to become a member?",
      Html("We ask you to sign in so we know who you are and ensure that we apply the correct discounts and privileges when you buy tickets to an event - Partners and Patrons get early booking and 20% discount on tickets to all Guardian Live events."),
      "why-sign-in-to-become-a-member"
    ),
    Item("What is Guardian Live?",
      Html("Guardian Live is a rolling events programme of discussions, debates, interviews and festivals that transform our journalism in print and online into live experiences. Tickets can only be booked by Guardian members."),
      "what-is-guardian-live"
    ),
    Item("Can I buy a ticket to a Guardian Live event without becoming a member?",
      Html("You can only buy a ticket to a Guardian Live event if you are a member. Access to these events is one of the benefits we offer members in return for their support. Our basic tier of membership is free."),
      "can-I-buy-without-becoming-member"
    ),
    Item("Can I buy a ticket to an event without signing in?",
      Html("You are required to sign in before buying tickets for Guardian Live events."),
      "can-I-buy-without-signing-in"
    ),
    Item("How many Guardian Live tickets can I buy for each event?",
      Html("You can buy as many tickets as you want. Partners and Patrons are entitled to two tickets at a discount. After you’ve bought your discounted tickets you can come back and buy as many as you want at full price. We are working to improve this so you can buy all your tickets in one go."),
      "how-many-tickets-can-i-buy"
    ),
    Item("How will I know what Guardian Live events are coming up?",
      Html("Our <a href='events'>events listing page</a> is updated regularly and when you join membership you will receive a weekly email newsletter containing information about upcoming events."),
      "what-events-are-upcoming"
    ),
    Item("What happens if I don't want to receive the weekly email?",
      Html("If you no longer want to receive the weekly email newsletter, click the unsubscribe link at the bottom of the email."),
      "i-do-not-want-weekly-email"
    ),
    Item("I am under 18 - can I still be a member?",
      Html("You have to be 18 or over to join."),
      "under-18"
    ),
    Item("I don’t live in London - can I still be a member?",
      Html("Yes, Guardian Membership is open to everyone. Guardian Live events at launch will be taking place in London, Edinburgh, Manchester and Bristol, and we have plans to run future events across the UK."),
      "do-not-live-in-london"
    ),
    Item("I don’t live in the UK - can I still be a member?",
      Html("Yes, Guardian Membership is open to anyone living anywhere in the world. And although our home is London, we will be extending membership and staging events for members in the USA and Australia."),
      "do-not-live-in-uk"
    ),
    Item("What happens if I want to change my membership tier?",
      Html(s"You can change your membership tier online. Just go to your <a href='${Config.idWebAppUrl}/membership/edit'>Profile</a> page on the membership site and follow the instructions."),
      "change-membership-tier"
    ),
    Item("What happens if I want to cancel my membership?",
      Html("You can cancel your membership by following the instructions on this <a href='tier/cancel'>page</a>. Your membership will run until the end of your current payment term."),
      "cancel-membership"
    ),
    Item("I've got feedback about membership",
      Html("Please fill in the feedback form on the site <a href='feedback'>here</a>."),
      "feedback-about-membership"
    ),
    Item("How do I know you are protecting my personal information?",
      Html("We will never use your information for any purpose without your permission. You can read more about our approach to data security in our privacy policy <a href='http://www.theguardian.com/help/privacy-policy'>here</a>."),
      "personal-data-protection"
    ),
    Item("What are the terms and conditions of membership?",
      Html("You can read the full set of terms and conditions <a href='http://www.theguardian.com/info/2014/sep/09/guardian-membership-terms-and-conditions'>here</a>."),
      "terms-conditions-of-membership"
    ),
    Item("How can I find out about disabled/wheelchair access for the venue?",
      Html("We only run events at venues that are accessible by wheelchair. You can tell us that you require accessibility information on the ticket booking form and we will contact you to go over the access route before the event."),
      "wheelchair-access-for-venue"
    ),
    Item("I've got a question",
      Html(s"Please email <a href='mailto:${Config.membershipSupport}'>${Config.membershipSupport}</a> with your question. We will do our best to get back to you within 24 hours. Alternatively, you can call the Guardian Membership customer services team on 0330 333 6898 from 8am to 5.30pm Monday to Friday and 8.30am to 12.30pm at weekends."),
      "have-a-question"
    ),
    Item("What are Guardian Masterclasses?",
      Html("Guardian Masterclasses match our readers’ interests with a wide range of courses and workshops, harnessing the expertise and specialisms of award-winning Guardian professionals and leading figures from the creative and digital industries."),
      "what-are-guardian-masterclasses"
    ),
    Item("Can I book a Guardian Masterclass course without becoming a member?",
      Html("Yes, Guardian Masterclasses are available to everyone from the <a href='http://www.theguardian.com/guardian-masterclasses'>masterclasses website</a>. However, only Partners and Patrons of the Guardian can enjoy 20% off. In order to receive this discount Masterclasses must be booked through the <a href='https://membership.theguardian.com/masterclasses'>Membership Masterclasses listings page</a>."),
      "can-I-book-without-becoming-member"
    ),
    Item("How many Guardian Masterclasses places can I book for each course?",
      Html("Yes, Guardian Masterclasses are available to everyone from the <a href='http://www.theguardian.com/guardian-masterclasses'>masterclasses website</a>. However, only Partners and Patrons of the Guardian can enjoy 20% off. In order to receive this discount Masterclasses must be booked through the <a href='https://membership.theguardian.com/masterclasses'>Membership Masterclasses listings page</a>."),
      "how-many-places-per-course"
    ),
    Item("How will I know what Guardian Masterclasses events are coming up?",
      Html("Our <a href='https://membership.theguardian.com/masterclasses'>Masterclasses listings page</a> is updated regularly and when you join membership you will receive a weekly email newsletter containing information about upcoming courses."),
      "what-events-are-coming-up"
    ),
    Item("Where do Guardian Masterclasses take place?",
      Html("Our Masterclasses are held in the meeting rooms and conference spaces of the Guardian's head quarters in King's Cross, London. <a href='http://www.theguardian.com/guardian-masterclasses/how-to-find-us?guni=Article:in%20body%20link'>Click here</a> for maps to help you find your way to the Guardian offices, and find your way around inside. We also host Masterclasses in other venues in London and beyond."),
      "masterclasses-location"
    ),
    Item("What is the Guardian Masterclasses refund policy?",
      Html("You may cancel a Guardian Masterclass if your notification is received by the Masterclasses team a minimum of 14 days prior to the start of the Guardian Masterclass. Provided the Masterclasses team has received your notice of cancellation within this time frame (and acknowledged your notice), Masterclasses will refund any fees received from you less the deposit where applicable. Please ensure the Masterclasses team has acknowledged your notice of cancellation. We will not be able to refund you where we did not receive your notice 14 days before the start of the Guardian Masterclass (regardless of when you sent it). Our tickets are not transferable to other courses or dates. The masterclass team contact details are available <a href='http://www.theguardian.com/guardian-masterclasses/about-masterclasses'>here</a>"),
      "masterclasses-refund-policy"
    ),
    Item("Where can I get more information on Guardian Masterclasses?",
      Html("More frequently asked questions are available <a href='http://www.theguardian.com/guardian-masterclasses/faqs'>here</a>"),
      "masterclasses-information"
    )
  )

  val subscribers = List(
    Item("Can I cancel membership during my trial?",
      Html("Yes, There is no obligation to continue to a paid Membership after your free 6 months and you can cancel at any time during the trial period."),
      "cancel-trial"
    ),
    Item("I'm a subscriber and I'm also a member, will I get this offer too?",
      Html(s"Yes, you may be entitled to receive this offer, please call customer services team on 0330 333 6898 or email <a href='mailto:${Config.membershipSupport}'>${Config.membershipSupport}</a>"),
      "existing-subscriber-member"
    ),
    Item("How much does membership cost normally?",
      Html("The Friend tier of Guardian Membership is free. Partners pay £15 per month or £135 per year if you pay in one go. For Patrons, the cost is £60 per month or £540 per year. If you choose the annual payment you get 3 months free when compared to the monthly price."),
      "normal-cost"
    ),
    Item("I don’t have an email address, can I still join?",
      Html("No, you need an email address in order to join Membership. Online registration allows you to access benefits, book tickets and receive regular Membership updates."),
      "no-email"
    ),
    Item("What if my subscription ends during my free Membership trial? ",
      Html("Your Partner Membership will be unaffected by  any changes made to your current subscription."),
      "subscription-ends"
    ),
    Item("Can I join Membership over the phone?",
      Html("No, In order to receive the benefits of Membership you need to complete online registration. However, our support team are available to talk you through the steps. Please c​all 0330 333 6898 f​rom 8am to 5.30pm Monday to Friday and 8.30am to 12.30pm at weekends."),
      "phone-joining"
    ),
    Item("I don’t live in London - can I still be a member?",
      Html("Yes, Guardian Membership is open to everyone. Guardian Live events at launch will be taking place in London, Edinburgh, Manchester and Bristol, and we have plans to run future events across the UK."),
      "outside-london"
    ),
    Item("I don’t live in the UK - can I still be a member?",
      Html("Yes, Guardian Membership is open to anyone living anywhere in the world. And although our home is London, we will be extending membership and staging events for members in the USA and Australia."),
      "outside-uk"
    ),
    Item("We will never use your information for any purpose without your permission. You can read more about our approach to data security in our privacy policy h​ere.​",
      Html("We will never use your information for any purpose without your permission. You can read more about our approach to data security in our <a href='http://www.theguardian.com/help/privacy-policy'>privacy policy</a>.​"),
      "privacy"
    ),
    Item("I have a question",
      Html(s"Please email <a href='mailto:${Config.membershipSupport}'>${Config.membershipSupport}</a> with your question. We will do our best to get back to you within 24 hours. Alternatively, you can call the Guardian Membership customer services team on 0330 333 6898 from 8am to 5.30pm Monday to Friday and 8.30am to 12.30pm at weekends."),
      "questions"
    )
  )

}
