package views.support

import com.gu.memsub.promo.{Bottom, Centre, HeroImageAlignment => Alignment, Top}

object HeroImageAlignment {

  implicit class ToCssName(heroImageAlignment: Alignment) {

    def cssName: String = heroImageAlignment match {
      case Centre => ""
      case Bottom => "hero-banner__image--bottom"
      case Top => "hero-banner__image--top"
    }

  }

}
