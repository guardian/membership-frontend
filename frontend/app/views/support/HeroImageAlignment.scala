package views.support

import com.gu.memsub.promo.{Bottom, Centre, HeroImageAlignment, Top}

object HeroImageAlignment {

  implicit class ToCssName(heroImageAlignment: HeroImageAlignment) {

    def cssName: String = heroImageAlignment match {
      case Centre => ""
      case Bottom => "hero-banner__image--bottom"
      case Top => "hero-banner__image--top"
    }

  }

}
