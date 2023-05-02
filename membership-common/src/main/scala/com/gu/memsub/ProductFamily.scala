package com.gu.memsub
import scalaz.syntax.std.option._
import scalaz.syntax.std.boolean._

sealed trait ProductFamily {
  val id: String
}

object ProductFamily {

  def fromId(id: String): Option[ProductFamily] = id match {
    case Subscriptions.id => Some(Subscriptions)
    case Membership.id => Some(Membership)
    case Contributions.id => Some(Contributions)
    case _ => None
  }
}

case object Subscriptions extends ProductFamily {
  override val id = "digitalpack"
}
case object Membership extends ProductFamily {
  override val id = "membership"
}
case object Contributions extends ProductFamily {
  override val id = "contributions"
}

/**
  * This is an enumeration of the products we have in Zuora,
  * where products contain multiple rate plans
  *
  * We need this in our model of a plan as paper delivery / paper voucher plans
  * have exactly the same structure but are nested under different zuora products
  */
sealed trait Product {
  val name: String
}
object Product {

  sealed trait ContentSubscription extends Product

  sealed trait Paper extends ContentSubscription
  sealed trait Weekly extends Paper


  case object Membership extends Product {
    val name = "membership"
  }
  case object GuardianPatron extends Product {
    val name = "guardianpatron"
  }
  case object SupporterPlus extends ContentSubscription {
    val name = "supporterPlus"
  }
  case object Digipack extends ContentSubscription {
    val name = "digitalpack"
  }
  case object Delivery extends Paper {
    val name = "delivery"
  }
  case object Voucher extends Paper {
    val name = "voucher"
  }
  case object DigitalVoucher extends Paper {
    val name = "digitalVoucher"
  }
  case object WeeklyZoneA extends Weekly {
    val name = "weeklyZoneA"
  }
  case object WeeklyZoneB extends Weekly {
    val name = "weeklyZoneB"
  }
  case object WeeklyZoneC extends Weekly {
    val name = "weeklyZoneC"
  }
  case object WeeklyDomestic extends Weekly {
    val name = "weeklyDomestic"
  }
  case object WeeklyRestOfWorld extends Weekly {
    val name = "weeklyRestOfWorld"
  }
  case object Contribution extends Product {
    val name = "contribution"
  }



  def fromId(id: String): Option[Product] = id match {
    case Digipack.name => Some(Digipack)
    case SupporterPlus.name => Some(SupporterPlus)
    case Membership.name => Some(Membership)
    case Delivery.name => Some(Delivery)
    case Voucher.name => Some(Voucher)
    case DigitalVoucher.name => Some(DigitalVoucher)
    case WeeklyZoneA.name => Some(WeeklyZoneA)
    case WeeklyZoneB.name => Some(WeeklyZoneB)
    case WeeklyZoneC.name => Some(WeeklyZoneC)
    case Contribution.name => Some(Contribution)
    case GuardianPatron.name => Some(GuardianPatron)
    case _ => None
  }

  type Membership = Membership.type
  type GuardianPatron = GuardianPatron.type
  type ZDigipack = Digipack.type
  type SupporterPlus = SupporterPlus.type
  type Delivery = Delivery.type
  type Voucher = Voucher.type
  type DigitalVoucher = DigitalVoucher.type
  type WeeklyZoneA = WeeklyZoneA.type
  type WeeklyZoneB = WeeklyZoneB.type
  type WeeklyZoneC = WeeklyZoneC.type
  type WeeklyDomestic = WeeklyDomestic.type
  type WeeklyRestOfWorld = WeeklyRestOfWorld.type
  type Contribution = Contribution.type
}

sealed trait Benefit {
  val id: String
  val isPhysical: Boolean // i.e. needs delivery address
  override def toString = s"$id (isPhysical? = $isPhysical)"
}

object Benefit {

  def fromId(id: String): Option[Benefit] =
    PaperDay.fromId(id) orElse
    FreeMemberTier.fromId(id) orElse
    PaidMemberTier.fromId(id) orElse
    (id == SupporterPlus.id).option(SupporterPlus) orElse
    (id == Digipack.id).option(Digipack) orElse
    (id == Adjustment.id).option(Adjustment) orElse
    (id == Contributor.id).option(Contributor) orElse
    (id == Weekly.id).option(Weekly)


  sealed trait MemberTier extends Benefit
  sealed trait FreeMemberTier extends MemberTier
  sealed trait PaidMemberTier extends MemberTier {
    override val isPhysical: Boolean = true
  }
  sealed trait PaperDay extends Benefit {
    override val isPhysical: Boolean = true
    val dayOfTheWeekIndex: Int
  }

  object PaperDay {
    def fromId(id: String): Option[PaperDay] = id match {
      case MondayPaper.id => MondayPaper.some
      case TuesdayPaper.id => TuesdayPaper.some
      case WednesdayPaper.id => WednesdayPaper.some
      case ThursdayPaper.id => ThursdayPaper.some
      case FridayPaper.id => FridayPaper.some
      case SaturdayPaper.id => SaturdayPaper.some
      case SundayPaper.id => SundayPaper.some
      case _ => None
    }
  }

  object FreeMemberTier {
    def fromId(id: String): Option[FreeMemberTier] = id match {
      case Friend.id => Friend.some
      case Staff.id => Staff.some
      case _ => None
    }
  }

  object PaidMemberTier {
    def fromId(id: String): Option[PaidMemberTier] = id match {
      case Supporter.id => Supporter.some
      case Partner.id => Partner.some
      case Patron.id => Patron.some
      case _ => None
    }
  }


  object Friend extends FreeMemberTier {
    override val id = "Friend"
    override val isPhysical: Boolean = false
  }

  object Contributor extends Benefit {
    override val id = "Contributor"
    override val isPhysical: Boolean = false
  }

  object Staff extends FreeMemberTier {
    override val id = "Staff"
    override val isPhysical: Boolean = true
  }

  object Supporter extends PaidMemberTier {
    override val id = "Supporter"
  }

  object Partner extends PaidMemberTier {
    override val id = "Partner"
  }

  object Patron extends PaidMemberTier {
    override val id = "Patron"
  }

  // This is the new non-membership version of a patron
  object GuardianPatron extends Benefit {
    override val id = "Guardian Patron"
    override val isPhysical = false // not sure if this should actually be true
  }

  object Digipack extends Benefit {
    override val id = "Digital Pack"
    override val isPhysical: Boolean = false
  }

  object SupporterPlus extends Benefit {
    override val id = "Supporter Plus"
    override val isPhysical: Boolean = false
  }

  object Weekly extends Benefit {
    override val id = "Guardian Weekly"
    override val isPhysical: Boolean = true
  }

  object MondayPaper extends PaperDay {
    override val id = "Print Monday"
    override val dayOfTheWeekIndex = 1
  }

  object TuesdayPaper extends PaperDay {
    override val id = "Print Tuesday"
    override val dayOfTheWeekIndex = 2
  }

  object WednesdayPaper extends PaperDay {
    override val id = "Print Wednesday"
    override val dayOfTheWeekIndex = 3
  }

  object ThursdayPaper extends PaperDay {
    override val id = "Print Thursday"
    override val dayOfTheWeekIndex = 4
  }

  object FridayPaper extends PaperDay {
    override val id = "Print Friday"
    override val dayOfTheWeekIndex = 5
  }

  object SaturdayPaper extends PaperDay {
    override val id = "Print Saturday"
    override val dayOfTheWeekIndex = 6
  }

  object SundayPaper extends PaperDay {
    override val id = "Print Sunday"
    override val dayOfTheWeekIndex = 7
  }

  object Adjustment extends Benefit {
    override val id = "Adjustment"
    override val isPhysical: Boolean = false
  }

}
