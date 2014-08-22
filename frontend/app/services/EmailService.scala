package services

import scala.util.{Success, Failure, Try}

import com.amazonaws.services.simpleemail._
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.regions.{Region, Regions}

import play.api.Logger

import configuration.Config
import forms.MemberForm.FeedbackForm

trait EmailService {
  val feedbackAddress: String

  val client = new AmazonSimpleEmailServiceClient()
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def sendFeedback(feedback: FeedbackForm) = {
    val to = new Destination().withToAddresses(feedbackAddress)
    val subjectContent = new Content("Membership feedback")

    val body =
      s"""
        Category: ${feedback.category}<br />
        Page: ${feedback.page}<br />
        Comments: ${feedback.feedback}<br />
        <br />
        Name: ${feedback.name}<br />
        Email address: ${feedback.email}
      """.stripMargin

    val bodyContent = new Body().withHtml(new Content(body))
    val message = new Message(subjectContent, bodyContent)
    val email = new SendEmailRequest(feedbackAddress, to, message)

    Try {
      client.sendEmail(email)
    } match {
      case Success(_) =>
      case Failure(error) =>
        Logger.debug(s"Failed to send feedback, got ${error.getMessage}")
    }
  }
}

object EmailService extends EmailService {
  val feedbackAddress = Config.membershipFeedback
}
