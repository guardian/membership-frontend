package model

import com.github.nscala_time.time.Imports._

case class Member(userId: String,
                  tier: Tier.Tier,
                  customerId: String,
                  joinDate: DateTime,
                  cancellationRequested: Boolean = false)

