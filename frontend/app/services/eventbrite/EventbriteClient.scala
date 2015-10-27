package services.eventbrite

import com.gu.membership.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import configuration.Config
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import play.api.libs.json.Reads
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventbriteClient(apiToken: String, maxDiscountQuantityAvailable: Int, val wsMetrics: StatusMetrics) extends WebServiceHelper[EBObject, EBError]  {

  val wsUrl = Config.eventbriteApiUrl
  def wsPreExecute(req: WSRequest): WSRequest = req.withQueryString("token" -> apiToken)

  def getAll[T](url: String, params: Seq[(String, String)] = Seq.empty)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    def getPage(page: Int) = get[EBResponse[T]](url, Seq("page" -> page.toString) ++ params:_*)

    for {
      initialResponse <- getPage(1)
      followingResponses: Seq[EBResponse[T]] <- Future.traverse(2 to initialResponse.pagination.page_count)(getPage)
    } yield (initialResponse +: followingResponses).flatMap(_.data)
  }
  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[Option[EBAccessCode]] = {
    val uri = s"events/${event.id}/access_codes"

    for {
      discounts <- getAll[EBAccessCode](uri) if ticketClasses.nonEmpty
      discount <- discounts.find(_.code == code).fold {
        post[EBAccessCode](uri, Map(
          "access_code.code" -> Seq(code),
          "access_code.quantity_available" -> Seq(maxDiscountQuantityAvailable.toString),
          "access_code.ticket_ids" -> Seq(ticketClasses.map(_.id).mkString(","))
        ))
      }(Future.successful)
    } yield Some(discount)
  } recover { case _: NoSuchElementException => None }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id", "expand" -> EBOrder.expansions.mkString(","))

}
