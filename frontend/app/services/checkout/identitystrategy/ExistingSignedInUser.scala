package services.checkout.identitystrategy

import model.{IdMinimalUser, UpdateIdUser}
import com.gu.identity.model.{User => IdUser}
import controllers.IdentityRequest
import forms.MemberForm.CommonForm
import play.api.mvc.Result
import services.IdentityService

import scala.concurrent.{ExecutionContext, Future}

case class ExistingSignedInUser(userId: IdMinimalUser, formData: CommonForm)(implicit idReq: IdentityRequest, identityService: IdentityService) extends Strategy {

  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result])(implicit executionContext: ExecutionContext) = {
    val fieldsFromForm = IdentityService.privateFieldsFor(formData)

    for (password <- formData.password) {
      identityService.updateUserPassword(password) // Update user password (social signin)
    }

    val updateUserF = identityService.updateUser(UpdateIdUser(privateFields = Some(fieldsFromForm)), userId.id).value

    for {
      fullIdUser <- identityService.getFullUserDetails(userId)
      _ <- updateUserF
      result <- checkoutFunc(fullIdUser.copy(privateFields = fieldsFromForm))
    } yield result
  }
}
