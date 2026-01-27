package users.controllers

import commons.services.ActionRunner
import authentication.api._
import authentication.exceptions.{InvalidPasswordException, MissingSecurityUserException}
import authentication.models.CredentialsWrapper
import commons.controllers.RealWorldAbstractController
import users.models.{UserDetailsWithToken, UserDetailsWithTokenWrapper}
import users.services.{RedirectService, UserService}
import play.api.libs.json._
import play.api.mvc._

class LoginController(actionRunner: ActionRunner,
                      authenticator: Authenticator[CredentialsWrapper],
                      components: ControllerComponents,
                      userService: UserService,
                      redirectService: RedirectService) extends RealWorldAbstractController(components) {
  def login(maybeRedirectUrl: Option[String]): Action[CredentialsWrapper] = Action.async(validateJson[CredentialsWrapper]) { request =>
    val email = request.body.user.email
    val loginAction = authenticator.authenticate(request)
      .zip(userService.getUserDetails(email))
      .map(tokenAndUserDetails => UserDetailsWithToken(tokenAndUserDetails._2, tokenAndUserDetails._1))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_))

    actionRunner.runTransactionally(loginAction)
      .map(result => {
        maybeRedirectUrl match {
          case Some(redirectUrl) => redirectService.performRedirect(redirectUrl)
          case None => result
        }
      })
      .recover({
        case _: InvalidPasswordException | _: MissingSecurityUserException =>
          val violation = JsObject(Map("email or password" -> Json.toJson(Seq("is invalid"))))
          val response = JsObject(Map("errors" -> violation))
          UnprocessableEntity(Json.toJson(response))
      })
  }

}