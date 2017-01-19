package model

import configuration.Email._

import scala.collection.immutable.Seq

trait FeedbackType {
  val description: String
  val email: String = membershipSupport
  def slug: String = description.filter(_.isLetterOrDigit).toLowerCase
}

object FeedbackType {
  val all: Seq[FeedbackType] = List(CustomerService, AccessibilityIssue, MembershipFeedback, GuardianLive, Journalism)
  def fromSlug(slug: String) = all.find(_.slug == slug)
}

case object CustomerService extends FeedbackType{
  override val description: String = "a customer service issue"
}

case object AccessibilityIssue extends FeedbackType {
  override val description: String = "an accessibility issue"
}

case object MembershipFeedback extends FeedbackType {
  override val description: String = "feedback on the Membership scheme"
  override val email: String = membershipFeedback
}

case object GuardianLive extends FeedbackType {
  override val description: String = "feedback on the Membership scheme"
}

case object Journalism extends FeedbackType {
  override val description = "feedback on our journalism"
  override val email: String = editorial
}
