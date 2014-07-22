package model

import com.github.nscala_time.time.Imports._

case class Member(salesforceContactId: String,
                  identityId: String,
                  tier: Tier.Tier,
                  stripeCustomerId: Option[String],
                  joinDate: DateTime,
                  optedIn: Boolean)
