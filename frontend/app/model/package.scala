import com.gu.memsub.Subscription
import com.gu.memsub.Subscription.{Paid, Free}
import com.gu.{salesforce => sf}

package object model {
  type PaidSubscription = Subscription with Paid
  type FreeSubscription = Subscription with Free
  type GenericSFContact = sf.Contact
}
