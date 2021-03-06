package services

import java.util

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model._
import utils.LegacyHashing
import com.gu.aws.CredentialsProvider
import model.IdMinimalUser
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import forms.FeedbackForm
import scala.util.{Failure, Success, Try}

trait FeedbackEmailService {

  val client = AmazonSimpleEmailServiceClientBuilder.standard
    .withCredentials(CredentialsProvider)
    .withRegion(Regions.EU_WEST_1)
    .build()

  def md5(input: String): String = {
    val hf = LegacyHashing.md5()
    util.Arrays.toString(hf.newHasher().putBytes(input.getBytes("UTF-8")).hash().asBytes())
  }
  def sendFeedback(feedback: FeedbackForm, userOpt: Option[IdMinimalUser], userEmail:Option[String], uaOpt: Option[String]) = {
    if (md5(feedback.email) == "[33, -110, 33, 95, -127, -114, 55, -110, 100, -54, 104, -58, 2, 10, 47, 111]") {
      SafeLogger.info("discarding email we can't do anything useful with")
    } else {
      val to = new Destination().withToAddresses(feedback.category.email)
      val subjectContent = new Content(s"Membership feedback from ${feedback.name}")
      val name = userOpt.map(_.displayName).mkString
      val id = userOpt.map(_.id).mkString
      val body =
        s"""
        Category: ${feedback.category.description}<br />
        Page: ${feedback.page}<br />
        Comments: ${feedback.feedback}<br />
        <br />
        Name: ${feedback.name}<br />
        Email address: ${feedback.email}<br />
        Identity user: ${name} ${userEmail.mkString} (${id})<br/>
        User agent: ${uaOpt.mkString}
      """.stripMargin
      val message = new Message(subjectContent, new Body().withHtml(new Content(body)))
      val email = new SendEmailRequest(feedback.category.email, to, message).withReplyToAddresses(feedback.email)

      Try {
        client.sendEmail(email)
      } match {
        case Success(details) => //all good
        case Failure(error) => SafeLogger.error(scrub"Failed to send feedback, got ${error.getMessage}")
      }
    }
  }
}

object EmailService extends FeedbackEmailService
