package services

import com.gu.contentapi.client.model.{Asset, Content}
import org.joda.time.DateTime

case class MembersOnlyContentData(webUrl: String, webTitle: String, webPublicationDateOpt: Option[DateTime],
                                  secureThumbnail: Option[String], trailText: Option[String], assets: List[Asset]) {
  val imgUrl = assets.find(_.typeData.get("width") == Some("460")).flatMap(_.file)
}

object MembersOnlyContentDataExtractor {
  def extractDetails(content: Content): Seq[MembersOnlyContentData] = {
    val assets = content.elements.flatMap(_.find(_.relation == "main")).fold(List[Asset]())(_.assets)
    val secureThumbnail = content.fields.flatMap(_.get("secureThumbnail"))
    val trailText = content.fields.flatMap(_.get("trailText"))

    Seq(MembersOnlyContentData(content.webUrl, content.webTitle, content.webPublicationDateOption, secureThumbnail,
      trailText, assets))
  }
}
