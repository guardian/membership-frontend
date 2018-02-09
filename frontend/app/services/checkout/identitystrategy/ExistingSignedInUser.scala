package services.checkout.identitystrategy

import com.gu.identity.play.idapi.UpdateIdUser
import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.typesafe.scalalogging.LazyLogging
import controllers.IdentityRequest
import forms.MemberForm.CommonForm
import play.api.mvc.{RequestHeader, Result}
import services.AuthenticationService.authenticatedIdUserProvider
import services.IdentityService

import scala.concurrent.{ExecutionContext, Future}

case class ExistingSignedInUser(userId: IdMinimalUser, formData: CommonForm)(implicit idReq: IdentityRequest, identityService: IdentityService) extends Strategy with LazyLogging {

  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result])(implicit executionContext: ExecutionContext) = {
    val fieldsFromForm = Some(IdentityService.privateFieldsFor(formData))

    for (password <- formData.password) {
      identityService.updateUserPassword(password) // Update user password (social signin)
    }

    val updateUserF = identityService.updateUser(UpdateIdUser(privateFields = fieldsFromForm), userId.id).value

    for {
      fullIdUser <- identityService.getFullUserDetails(userId)
      _ <- updateUserF
      result <- checkoutFunc(fullIdUser.copy(privateFields = fieldsFromForm))
    } yield result
  }
}
