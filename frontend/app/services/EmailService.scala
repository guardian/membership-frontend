package services

import java.math.BigInteger
import java.security.MessageDigest
import java.util

import com.amazonaws.regions.{Region, Regions}
import com.typesafe.scalalogging.LazyLogging
import com.gu.identity.play.IdMinimalUser
import com.amazonaws.services.simpleemail._
import com.amazonaws.services.simpleemail.model._
import com.google.common.hash.{HashCode, HashFunction, Hashing}
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

  def md5(input: String): String = {
    val hf = Hashing.md5()
    util.Arrays.toString(hf.newHasher().putBytes(input.getBytes("UTF-8")).hash().asBytes())
  }
  def sendFeedback(feedback: FeedbackForm, userOpt: Option[IdMinimalUser], uaOpt: Option[String]) = {
    if (md5(feedback.email) == "[33, -110, 33, 95, -127, -114, 55, -110, 100, -54, 104, -58, 2, 10, 47, 111]") {
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
