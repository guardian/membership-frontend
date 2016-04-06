package model

import model.RichEvent.RichEvent
import views.support.Asset

object ProviderLogo {
  def apply(event: RichEvent): ProviderLogo = {
    event.providerOpt.getOrElse(ProviderLogo(
            event.metadata.identifier,
            event.metadata.title,
            Asset.at(s"images/providers/${event.metadata.identifier}.svg")
        ))
  }
}

case class ProviderLogo(
    id: String,
    title: String,
    path: String
    )
