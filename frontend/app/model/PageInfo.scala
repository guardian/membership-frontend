package model
import views.support.Asset
import configuration.{Config, CopyConfig}

case class PageInfo(
  title: String,
  url: String,
  description: Option[String],
  image: Option[String] = Some(PageInfo.defaultImage),
  stripePublicKey: Option[String] = None
)

object PageInfo {
  val defaultImage = Config.membershipUrl + Asset.at("images/common/mem-promo.jpg")

  // url has the domain prepended in templates
  val default = PageInfo(
    CopyConfig.copyTitleDefault,
    "/",
    Some(CopyConfig.copyDescriptionDefault)
  )
}
