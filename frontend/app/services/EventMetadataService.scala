package services

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, _}
import model.EventMetadata
import utils.awswrappers.dynamodb._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import configuration.Config
import scala.concurrent.Future

object EventMetadataService {

  // TODO: Environment specific
  val tableName = "DEV_metadata"

  val dynamoDbClient: AmazonDynamoDBAsyncClient = {
    new AmazonDynamoDBAsyncClient(Config.awsCredentialsProvider)
      .withRegion(Regions.EU_WEST_1)
  }

  def list() = {
    dynamoDbClient.scanFuture(new ScanRequest()
      .withTableName(tableName)
    ) map { result =>
      (result.getItems.asScala.toSeq map { item =>
        EventMetadata.fromAttributeValueMap(item.asScala.toMap)
      }).flatten
    }
  }

  def get(id: String): Future[Option[EventMetadata]] = {

    if (Config.eventMetadataEnabled) {
      dynamoDbClient.getItemFuture(new GetItemRequest()
        .withTableName(tableName)
        .withKey(Map(
          "ticketingProviderId" -> new AttributeValue().withS(id),
          "ticketingProvider" -> new AttributeValue().withS("eventbrite")
        ).asJava)
      ) map { result =>
        for {
          item <- Option(result.getItem)
          entry <- EventMetadata.fromAttributeValueMap(item.asScala.toMap)
        } yield entry
      }
    } else {
      Future.successful(None)
    }

  }

  def create(entry: EventMetadata) = {
    // TODO: Handle optional fields
    dynamoDbClient.putItemFuture(new PutItemRequest()
      .withTableName(tableName)
      .withItem(Map(
        "ticketingProvider" -> new AttributeValue().withS(entry.ticketingProvider),
        "ticketingProviderId" -> new AttributeValue().withS(entry.ticketingProviderId),
        "gridUrl" -> new AttributeValue().withS(entry.gridUrl.getOrElse(""))
      ).asJava)
    )
  }

}
