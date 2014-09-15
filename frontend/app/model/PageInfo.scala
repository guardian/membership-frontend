package model
import views.support.Asset
import configuration.Config

case class PageInfo(
  title: String,
  url: String,
  description: Option[String],
  image: Option[String] = Some(PageInfo.defaultImage)
)

object PageInfo {
  val defaultImage = Config.membershipUrl + Asset.at("images/mem-promo.jpg")

  // url has the domain prepended in templates
  val default = PageInfo(
    Config.copyTitleDefault,
    "/",
    Some(Config.copyDescriptionDefault)
  )
}