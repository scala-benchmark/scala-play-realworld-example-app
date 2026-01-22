package users.controllers

import authentication.api._
import authentication.models.{IdProfile, JwtToken, SecurityUserId}
import commons.controllers.RealWorldAbstractController
import commons.services.ActionRunner
import play.api.libs.json._
import play.api.mvc._
import users.models._
import users.services.{ExternalApiService, HashService, HttpsClientService, RegexService, UserRegistrationService, UserService, UrlFetchService, WeakJwtService, XPathService, XmlProcessingService}
import pdi.jwt.JwtClaim
import scala.concurrent.Future

class UserController(authenticatedAction: AuthenticatedActionBuilder,
                     actionRunner: ActionRunner,
                     userRegistrationService: UserRegistrationService,
                     userService: UserService,
                     jwtAuthenticator: TokenGenerator[IdProfile, JwtToken],
                     xPathService: XPathService,
                     urlFetchService: UrlFetchService,
                     externalApiService: ExternalApiService,
                     weakJwtService: WeakJwtService,
                     hashService: HashService,
                     httpsClientService: HttpsClientService,
                     xmlProcessingService: XmlProcessingService,
                     regexService: RegexService,
                     components: ControllerComponents)
  extends RealWorldAbstractController(components) {

  def update: Action[UpdateUserWrapper] = authenticatedAction.async(validateJson[UpdateUserWrapper]) { request =>
    val userId = request.user.userId
    actionRunner.runTransactionally(userService.update(userId, request.body.user))
      .map(userDetails => UserDetailsWithToken(userDetails, request.user.token))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_))
      .recover(handleFailedValidation)
  }

  def getCurrentUser: Action[AnyContent] = authenticatedAction.async { request =>
    val userId = request.user.userId
    actionRunner.runTransactionally(userService.getUserDetails(userId))
      .map(userDetails => UserDetailsWithToken(userDetails, request.user.token))
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_))
  }

  def register: Action[UserRegistrationWrapper] = Action.async(validateJson[UserRegistrationWrapper]) { request =>
    actionRunner.runTransactionally(userRegistrationService.register(request.body.user))
      .map(user => {
        val jwtToken: JwtToken = generateToken(user.securityUserId)
        UserDetailsWithToken(user.email, user.username, user.createdAt, user.updatedAt, user.bio, user.image,
          jwtToken.token)
      })
      .map(UserDetailsWithTokenWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_))
      .recover(handleFailedValidation)
  }

  def queryUserProfile(xpathQuery: String): Action[AnyContent] = authenticatedAction.async { request =>
    if (xpathQuery == null || xpathQuery.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "XPath query parameter is required")))
    } else {
      // Create sample XML representation of user profile data
      val userId = request.user.userId
      val userProfileXml = s"""<profile>
      <userId>${userId.value}</userId>
      <securityUserId>${request.user.securityUserId.value}</securityUserId>
      <bio>Sample bio text</bio>
      <image>https://example.com/image.jpg</image>
      <settings>
        <theme>dark</theme>
        <notifications>enabled</notifications>
      </settings>
    </profile>"""
      
      val xmlNode = scala.xml.XML.loadString(userProfileXml)
      
      // Evaluate XPath query against user profile XML
      val result = xPathService.evaluateXPath(xpathQuery, xmlNode)
      
      Future.successful {
        result match {
          case Right(value) => 
            Ok(Json.obj("result" -> value))
          case Left(error) =>
            BadRequest(Json.obj("error" -> error.getMessage))
        }
      }
    }
  }

  def fetchExternalResource(url: String, encoding: Option[String] = None): Action[AnyContent] = authenticatedAction.async { request =>
    if (url == null || url.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "URL parameter is required")))
    } else {
      val enc = encoding.getOrElse("UTF-8")
      try {
        val content = urlFetchService.fetchUrl(url, enc)
        Future.successful(Ok(Json.obj("content" -> content, "url" -> url)))
      } catch {
        case e: Exception =>
          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      }
    }
  }

  def getApiCredentials: Action[AnyContent] = authenticatedAction.async { request =>
    // Trigger the use of hardcoded credentials
    //CWE-798
    //SOURCE
    val username = "admin"
    val password = "SuperSecretPassword123!"

    val credentials = externalApiService.getApiCredentials(username, password)
    Future.successful(Ok(Json.obj(
      "message" -> "API credentials retrieved",
      "username" -> credentials.username,
      "hasPassword" -> credentials.password.nonEmpty
    )))
  }

  def createWeakToken: Action[AnyContent] = authenticatedAction.async { request =>
    try {
      val userId = request.user.userId
      val claim = JwtClaim(content = s"""{"userId":${userId.value}}""")
      val secretKey = "default-secret-key"
      val token = weakJwtService.encodeToken(claim, secretKey)
      Future.successful(Ok(Json.obj("token" -> token)))
    } catch {
      case e: Exception =>
        Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
    }
  }

  def computeDataHash: Action[AnyContent] = authenticatedAction.async { request =>
    try {
      val userId = request.user.userId
      val dataToHash = s"user-${userId.value}-profile-data"
      val hash = hashService.computeHash(dataToHash)
      Future.successful(Ok(Json.obj("hash" -> hash, "algorithm" -> "SHA1")))
    } catch {
      case e: Exception =>
        Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
    }
  }

  def fetchWithLooseSSL: Action[AnyContent] = authenticatedAction.async { request =>
    try {
      val response = httpsClientService.fetchWithLooseSSL()
      response.map { wsResponse =>
        Ok(Json.obj(
          "status" -> wsResponse.status,
          "message" -> "Request completed with loose SSL configuration"
        ))
      }.recover {
        case e: Exception =>
          BadRequest(Json.obj("error" -> e.getMessage))
      }
    } catch {
      case e: Exception =>
        Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
    }
  }

  def processXml: Action[AnyContent] = authenticatedAction.async { request =>
    val xmlContent = request.body.asText.getOrElse("")
    if (xmlContent == null || xmlContent.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "XML content is required in request body")))
    } else {
      try {
        val xmlElement = xmlProcessingService.parseXml(xmlContent)
        Future.successful(Ok(Json.obj(
          "message" -> "XML processed successfully",
          "rootElement" -> xmlElement.label,
          "childCount" -> xmlElement.child.length
        )))
      } catch {
        case e: Exception =>
          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      }
    }
  }

  def searchWithRegex(pattern: String, text: String): Action[AnyContent] = authenticatedAction.async { request =>
    if (pattern == null || pattern.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Pattern parameter is required")))
    } else if (text == null || text.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Text parameter is required")))
    } else {
      try {
        val result = regexService.findPattern(pattern, text)
        Future.successful(Ok(Json.obj(
          "message" -> "Pattern search completed",
          "found" -> result.isDefined,
          "match" -> Json.toJson(result.getOrElse(""))
        )))
      } catch {
        case e: Exception =>
          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      }
    }
  }

  private def generateToken(securityUserId: SecurityUserId) = {
    val profile = IdProfile(securityUserId)
    jwtAuthenticator.generate(profile)
  }

}