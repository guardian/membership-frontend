package model

import com.gu.contentapi.client.model.{Asset, Content}

case class MembersOnlyContent(content: Content) {
  val elementOpt = content.elements.flatMap(_.find(_.relation == "main"))
  val imgOpt = elementOpt.flatMap { element =>
    Some(ResponsiveImageGroup(
      None,
      element.assets.headOption.flatMap(_.typeData.get("altText")).fold("")(a => a),
      element.assets.flatMap { asset =>
        for {
         secureFile <- asset.file.map(_.replace("http://static", "https://static-secure"))
         width <- asset.typeData.get("width")
        } yield ResponsiveImage(secureFile, width.toInt)
      }
    ))
  }
  val tagTitle = content.tags.map { tag =>
    tag.id.toLowerCase match {
      case "type/quiz" | "tone/extraoffers" => tag.webTitle
      case _ => ""
    }
  }
}
