package services

import java.math.BigInteger
import java.security.MessageDigest

import com.amazonaws.regions.{Region, Regions}
import com.typesafe.scalalogging.LazyLogging
import com.gu.identity.play.IdMinimalUser
import com.amazonaws.services.simpleemail._
import com.amazonaws.services.simpleemail.model._
import configuration.Config
import forms.MemberForm.FeedbackForm

import scala.util.{Failure, Success, Try}

trait EmailService extends LazyLogging {
  val feedbackAddress: String

  val client = {
    val c = new AmazonSimpleEmailServiceClient()
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  private val digest = MessageDigest.getInstance("MD5")

  def md5(salt: String, input: String): Option[String] = {
    try {
      digest.update((salt + input).getBytes(), 0, (salt + input).length)

      Option(new BigInteger(1, digest.digest()).toString(16))
    } catch {
      case _: Throwable => None
    }
  }
  def sendFeedback(feedback: FeedbackForm, userOpt: Option[IdMinimalUser], uaOpt: Option[String]) = {
    if (md5("wxEKKfFtz4zQREhodbVvk84IzRR3iXLMCzGaIkxGv7YeLgSwEjqPPNei0e3B0kLo86b9ArS9ZgtLHzMp7qY2asot7feJYT9GQvxza95xif9mJy9kcIfncgKoPG1GloIV", feedback.email).contains("87bc0ff1050d56f06e50809a42f44bfc")) {
      logger.info("discarding email we can't do anything useful with")
    } else {
      logger.info(s"Sending feedback for ${feedback.name} - Identity $userOpt")
      val to = new Destination().withToAddresses(feedbackAddress)
      val subjectContent = new Content("Membership feedback")

      val body =
        s"""
        Category: ${feedback.category}<br />
        Page: ${feedback.page}<br />
        Comments: ${feedback.feedback}<br />
        <br />
        Name: ${feedback.name}<br />
        Email address: ${feedback.email}<br />
        Identity user: ${userOpt.mkString}<br />
        User agent: ${uaOpt.mkString}
      """.stripMargin

      val message = new Message(subjectContent, new Body().withHtml(new Content(body)))
      val email = new SendEmailRequest(feedbackAddress, to, message)

      Try {
        client.sendEmail(email)
      } match {
        case Success(details) => //all good
        case Failure(error) => logger.error(s"Failed to send feedback, got ${error.getMessage}")
      }
    }
  }
}

object EmailService extends EmailService {
  val feedbackAddress = Config.membershipFeedback
}
