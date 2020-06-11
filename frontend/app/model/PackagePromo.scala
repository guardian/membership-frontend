package model
import io.lemonlabs.uri.Uri

object PackagePromo {

  // the call-to-action button on the package promo view element
  case class CtaButton(text: String, to: Uri, attributes: Map[String, String])
}
