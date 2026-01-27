package users

import com.softwaremill.macwire.wire
import commons.config.{WithControllerComponents, WithExecutionContextComponents}
import commons.models.Username
import _root_.authentication.AuthenticationComponents
import play.api.{Configuration, Environment}
import play.api.routing.Router
import play.api.routing.sird._
import users.controllers._
import users.repositories.{FollowAssociationRepo, ProfileRepo, UserRepo}
import users.services._

trait UserComponents extends AuthenticationComponents with WithControllerComponents with WithExecutionContextComponents {
  
  def environment: Environment
  def configuration: Configuration
  def actorSystem: akka.actor.ActorSystem
  lazy val userController: UserController = wire[UserController]
  lazy val userService: UserService = wire[UserService]
  lazy val userRepo: UserRepo = wire[UserRepo]
  lazy val userRegistrationService: UserRegistrationService = wire[UserRegistrationService]
  lazy val userRegistrationValidator: UserRegistrationValidator = wire[UserRegistrationValidator]
  lazy val userUpdateValidator: UserUpdateValidator = wire[UserUpdateValidator]

  lazy val passwordValidator: PasswordValidator = wire[PasswordValidator]
  lazy val usernameValidator: UsernameValidator = wire[UsernameValidator]
  lazy val emailValidator: EmailValidator = wire[EmailValidator]

  lazy val profileController: ProfileController = wire[ProfileController]
  lazy val profileService: ProfileService = wire[ProfileService]
  lazy val profileRepo: ProfileRepo = wire[ProfileRepo]

  lazy val followAssociationRepo: FollowAssociationRepo = wire[FollowAssociationRepo]

  lazy val loginController: LoginController = wire[LoginController]
  lazy val redirectService: RedirectService = wire[RedirectService]
  lazy val xPathService: XPathService = wire[XPathService]
  lazy val urlFetchService: UrlFetchService = wire[UrlFetchService]
  lazy val externalApiService: ExternalApiService = wire[ExternalApiService]
  lazy val weakJwtService: WeakJwtService = wire[WeakJwtService]
  lazy val hashService: HashService = wire[HashService]
  lazy val httpsClientService: HttpsClientService = {
    implicit val mat: akka.stream.Materializer = akka.stream.Materializer(actorSystem)
    new HttpsClientService(environment, configuration)
  }
  lazy val xmlProcessingService: XmlProcessingService = wire[XmlProcessingService]
  lazy val regexService: RegexService = wire[RegexService]

  lazy val authenticatedAction: AuthenticatedActionBuilder = wire[JwtAuthenticatedActionBuilder]
  lazy val optionallyAuthenticatedAction: OptionallyAuthenticatedActionBuilder =
    wire[JwtOptionallyAuthenticatedActionBuilder]

  val userRoutes: Router.Routes = {
    case POST(p"/users") =>
      userController.register
    case GET(p"/user") =>
      userController.getCurrentUser
    case GET(p"/user/profile/query" ? q_o"xpath=$maybeXpathQuery") =>
      //CWE-643
      //SOURCE
      maybeXpathQuery match {
        case Some(xpathQuery) => userController.queryUserProfile(xpathQuery)
        case None => userController.queryUserProfile("")
      }
    case GET(p"/user/fetch" ? q"url=$url" & q_o"encoding=$maybeEncoding") =>
      //CWE-918
      //SOURCE
      userController.fetchExternalResource(url, maybeEncoding)
    case GET(p"/user/api/credentials") =>
      userController.getApiCredentials
    case POST(p"/user/token/weak") =>
      userController.createWeakToken
    case POST(p"/user/hash") =>
      userController.computeDataHash
    case GET(p"/user/fetch/loose-ssl") =>
      userController.fetchWithLooseSSL
    case POST(p"/user/xml/process") =>
      //CWE-611
      //SOURCE
      userController.processXml
    case GET(p"/user/regex/search" ? q"pattern=$pattern" & q"text=$text") =>
      //CWE-1333
      //SOURCE
      userController.searchWithRegex(pattern, text)
    case POST(p"/users/login" ? q_o"redirectUrl=$maybeRedirectUrl") =>
      //CWE-601
      //SOURCE
      loginController.login(maybeRedirectUrl)
    case PUT(p"/user") =>
      userController.update
    case GET(p"/profiles/$rawUsername") =>
      profileController.findByUsername(Username(rawUsername))
    case POST(p"/profiles/$rawUsername/follow") =>
      profileController.follow(Username(rawUsername))
    case DELETE(p"/profiles/$rawUsername/follow") =>
      profileController.unfollow(Username(rawUsername))
  }
}