package utils.awswrappers

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{Format, Json}
import scala.util.Try

object dynamodb {
  implicit class RichDynamoDbClient(dynamoDbClient: AmazonDynamoDBAsyncClient) {

    def updateItemFuture(updateItemRequest: UpdateItemRequest) =
      asFuture[UpdateItemRequest, UpdateItemResult](dynamoDbClient.updateItemAsync(updateItemRequest, _))

    def getItemFuture(getItemRequest: GetItemRequest) =
      asFuture[GetItemRequest, GetItemResult](dynamoDbClient.getItemAsync(getItemRequest, _))

    def queryFuture(queryRequest: QueryRequest) =
      asFuture[QueryRequest, QueryResult](dynamoDbClient.queryAsync(queryRequest, _))

    def scanFuture(scanRequest: ScanRequest) =
      asFuture[ScanRequest, ScanResult](dynamoDbClient.scanAsync(scanRequest, _))

    def putItemFuture(putItemRequest: PutItemRequest) =
      asFuture[PutItemRequest, PutItemResult](dynamoDbClient.putItemAsync(putItemRequest, _))

    def deleteItemFuture(deleteItemRequest: DeleteItemRequest) =
      asFuture[DeleteItemRequest, DeleteItemResult](dynamoDbClient.deleteItemAsync(deleteItemRequest, _))
  }

  implicit class RichAttributeMap(map: Map[String, AttributeValue]) {

    def getString(k: String): Option[String] =
      map.get(k).flatMap(v => Option(v.getS))

    def getLong(k: String): Option[Long] = {
      map.get(k).flatMap({ v =>
        val x = Try(v.getN.toLong)
        x.failed foreach { error => Logger.error(s"Error de-serializing $k as Long", error) }
        x.toOption
      })
    }

    def getDateTime(k: String): Option[DateTime] =
      getLong(k).map(n => new DateTime(n))

    def getJson(k: String) = getString(k).flatMap(s => Try(Json.parse(s)).toOption)

    def getSerializedJson[A](k: String)(implicit formatter: Format[A]) =
      getJson(k).flatMap(x => formatter.reads(x).asOpt)
  }
}
