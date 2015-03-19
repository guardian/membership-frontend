package model

import com.gu.contentapi.client.model.Content

case class MembersOnlyContent(content: Content) {
  val imgOpt = ResponsiveImageGroup(content)
  val tagTitle = content.tags.map { tag =>
    tag.id.toLowerCase match {
      case "type/quiz" | "tone/extraoffers" => tag.webTitle
      case _ => ""
    }
  }
}
