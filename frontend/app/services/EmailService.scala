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
import com.gu.aws.CredentialsProvider
import configuration.Config
import forms.FeedbackForm

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait EmailService extends LazyLogging {

  val client = {
    val c = new AmazonSimpleEmailServiceClient(CredentialsProvider)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  def md5(input: String): String = {
    val hf = Hashing.md5()
    util.Arrays.toString(hf.newHasher().putBytes(input.getBytes("UTF-8")).hash().asBytes())
  }
  def sendFeedback(feedback: FeedbackForm, userOpt: Option[IdMinimalUser], userEmail:Option[String], uaOpt: Option[String]) = {
    if (md5(feedback.email) == "[33, -110, 33, 95, -127, -114, 55, -110, 100, -54, 104, -58, 2, 10, 47, 111]") {
      logger.info("discarding email we can't do anything useful with")
    } else {
      logger.info(s"Sending feedback for ${feedback.name} - Identity $userOpt")
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
      logger.info(body)
      val message = new Message(subjectContent, new Body().withHtml(new Content(body)))
      val email = new SendEmailRequest(feedback.category.email, to, message)

      Try {
        client.sendEmail(email)
      } match {
        case Success(details) => //all good
        case Failure(error) => logger.error(s"Failed to send feedback, got ${error.getMessage}")
      }
    }
  }
}

object EmailService extends EmailService
