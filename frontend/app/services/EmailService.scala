package services

import scala.util.{Failure, Try}

import com.amazonaws.services.simpleemail._
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.regions.{Region, Regions}

import play.api.Logger

import configuration.Config

trait EmailService {
  val client = new AmazonSimpleEmailServiceClient()
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def sendEmail(from: String, body: String) = {
    val to = new Destination().withToAddresses(Config.membershipFeedback)
    val subject = new Content().withData("Membership feedback")
    val body = new Body().withText(body)
    val message = new Message().withSubject(subject).withBody(body)
    val email = new SendEmailRequest().withSource(from).withDestination(to).withMessage(message)

    Try {
      client.sendEmail(email)
    } match {
      case Failure(error) =>
        Logger.debug(s"Failed to send feedback from $from, got ${error.getMessage}")
    }
  }
}
