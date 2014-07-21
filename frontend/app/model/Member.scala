package model

import com.github.nscala_time.time.Imports._

case class Member(crmId: String,
                  identityId: String,
                  tier: Tier.Tier,
                  customerId: Option[String],
                  joinDate: DateTime,
                  optedIn: Boolean)

