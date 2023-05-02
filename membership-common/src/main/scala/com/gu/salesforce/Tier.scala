package com.gu.salesforce

sealed trait Tier {
  def name: String
  def slug = name.toLowerCase
  def isPublic: Boolean
  def isPaid: Boolean
}

sealed trait FreeTier extends Tier {
  def isPaid = false
}

sealed trait PaidTier extends Tier {
  def isPaid = true
}

object PaidTier {
  def all = Seq[PaidTier](Tier.Supporter(), Tier.Partner(), Tier.Patron())
  def slugMap = all.map(tier => tier.slug -> tier ).toMap
}

object Tier {
  case class Staff() extends FreeTier {
    override val name = "Staff"
    override def isPublic = false
  }

  case class Friend() extends FreeTier{
    override val name = "Friend"
    override def isPublic = true
  }

  case class Supporter() extends PaidTier {
    override val name = "Supporter"
    override def isPublic = true
  }

  case class Partner() extends PaidTier {
    override val name = "Partner"
    override def isPublic = true
  }

  case class Patron() extends PaidTier {
    override val name = "Patron"
    override def isPublic = true
  }

  // The order of this list is used in Ordered[Tier] above
  lazy val all = FreeTier.all ++ PaidTier.all
  lazy val allPublic = all.filter(_.isPublic)
  lazy val slugMap = all.map { tier => (tier.slug, tier) }.toMap
  lazy val nameMap = all.map { tier => (tier.name, tier) }.toMap

  val staff = Staff()
  val friend = Friend()
  val supporter = Supporter()
  val partner = Partner()
  val patron = Patron()
}

object FreeTier {
  def all = Seq[FreeTier](Tier.Friend(), Tier.Staff())
  def slugMap = all.map(tier => tier.slug -> tier ).toMap
}

