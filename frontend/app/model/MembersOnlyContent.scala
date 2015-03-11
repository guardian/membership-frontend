package model

import com.gu.contentapi.client.model.{Asset, Content}

case class MembersOnlyContent(content: Content, assetsMain: List[Asset], fields: Option[Map[String, String]]) {

  val sortedImageSizes = assetsMain.flatMap { asset =>
    for {
      file <- asset.file.map(_.replace("http://static", "https://static-secure"))
      width <- asset.typeData.get("width")
    } yield (file, width)
  }.sortBy(_._2.toInt)
  val secureThumbnail = content.fields.flatMap(_.get("secureThumbnail"))
  val mainImg = sortedImageSizes.lastOption.fold(secureThumbnail.getOrElse(""))(_._1)
  val srcset = sortedImageSizes.map { imageDetails =>
    val (secureFile, width) = imageDetails
    secureFile + " " + width + "w"
  }.mkString(", ")
  val tagTitle = content.tags.map { tag =>
    tag.id.toLowerCase match {
      case "type/quiz" | "tone/extraoffers" => tag.webTitle
      case _ => ""
    }
  }
}

object MembersOnlyContentExtractor {
  def extractDetails(content: Content): Seq[MembersOnlyContent] = {
    val assetsMain = content.elements.flatMap(_.find(_.relation == "main")).fold(List[Asset]())(_.assets)
    Seq(MembersOnlyContent(content, assetsMain, content.fields))
  }
}
