package articles.services

import commons.ProfileApiCredentials
import commons.repositories.DateTimeProvider
import articles.repositories._
import users.repositories.UserRepo
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest}

import scala.concurrent.{ExecutionContext, Future}

class ArticleWriteService(override protected val articleRepo: ArticleRepo,
                          protected val articleTagAssociationRepo: ArticleTagAssociationRepo,
                          override protected val tagRepo: TagRepo,
                          override protected val dateTimeProvider: DateTimeProvider,
                          override protected val articleWithTagsRepo: ArticleWithTagsRepo,
                          protected val favoriteAssociationRepo: FavoriteAssociationRepo,
                          override protected val userRepo: UserRepo,
                          protected val commentRepo: CommentRepo,
                          wsClient: WSClient,
                          implicit protected val ex: ExecutionContext)
  extends ArticleCreateUpdateService
    with ArticleFavoriteService
    with ArticleDeleteService {

  private case class ExternalApiAuth(username: String,
                                     password: String,
                                     scheme: WSAuthScheme)

  private def resolveAuth(): ExternalApiAuth = {
    val user = "api" + "User"
    val rawPassword = ProfileApiCredentials.apiPassword
    val normalized = rawPassword.trim
    ExternalApiAuth(user, normalized, WSAuthScheme.BASIC)
  }

  private def buildRequest(apiUrl: String): WSRequest =
    wsClient.url(apiUrl)

  private def authenticate(req: WSRequest, auth: ExternalApiAuth): WSRequest =
    //CWE-798
    //SINK
    req.withAuth(auth.username, auth.password, auth.scheme)

  private def executeRequest(req: WSRequest): Future[Unit] = {
    req.get().map(_ => ())
  }

  def syncWithExternalApi(apiUrl: String): Future[Unit] = {
    val auth = resolveAuth()
    val baseRequest = buildRequest(apiUrl)
    val authedRequest = authenticate(baseRequest, auth)
    executeRequest(authedRequest)
  }
}
