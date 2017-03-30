package services

import java.util

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import com.google.common.hash.Hashing
import com.gu.aws.CredentialsProvider
import com.gu.identity.play.IdMinimalUser
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import forms.FeedbackForm
import model.ContributorRow
import play.api.libs.json.Json
import utils.AwsAsyncHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.\/
import scalaz.syntax.either._

trait FeedbackEmailService extends LazyLogging {

  val client = AmazonSimpleEmailServiceClientBuilder.standard
    .withCredentials(CredentialsProvider)
    .withRegion(Regions.EU_WEST_1)
    .build()

  def md5(input: String): String = {
    val hf = Hashing.md5()
    util.Arrays.toString(hf.newHasher().putBytes(input.getBytes("UTF-8")).hash().asBytes())
  }
  def sendFeedback(feedback: FeedbackForm, userOpt: Option[IdMinimalUser], userEmail:Option[String], uaOpt: Option[String]) = {
    if (md5(feedback.email) == "[33, -110, 33, 95, -127, -114, 55, -110, 100, -54, 104, -58, 2, 10, 47, 111]") {
      logger.info("discarding email we can't do anything useful with")
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
        case Failure(error) => logger.error(s"Failed to send feedback, got ${error.getMessage}")
      }
    }
  }
}

trait ThankYouEmailService extends LazyLogging {
  private val sqsClient = AmazonSQSAsyncClientBuilder.standard.withCredentials(CredentialsProvider)
    .withRegion(Regions.EU_WEST_1)
    .build()

  private val thankYouQueueUrl = sqsClient.getQueueUrl(Config.thankYouEmailQueue).getQueueUrl

  def thankYou(row: ContributorRow): Future[\/[Throwable, SendMessageResult]] = {
    val payload = Json.stringify(Json.toJson(row))
    logger.info("email queue payload:\n" + payload)

    val handler = new AwsAsyncHandler[SendMessageRequest, SendMessageResult]
    sqsClient.sendMessageAsync(thankYouQueueUrl, payload, handler)

    handler.future.map { result =>
      result.right
    } recover {
      case t: Throwable =>
        logger.error(s"Unable to send message to the SQS queue $thankYouQueueUrl", t)
        t.left
    }
  }

}

object EmailService extends FeedbackEmailService with ThankYouEmailService
