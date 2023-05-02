import actions._
import services.TouchpointBackends
import services.api.MemberService

package object controllers {

  trait MemberServiceProvider {
    def memberService(implicit request: BackendProvider, tpbs: TouchpointBackends): MemberService =
      request.touchpointBackend.memberService
  }
}
