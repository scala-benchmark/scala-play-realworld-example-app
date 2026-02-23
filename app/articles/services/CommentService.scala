package articles.services

import articles.exceptions.AuthorMismatchException
import articles.models._
import articles.repositories._
import commons.repositories.DateTimeProvider
import kantan.xpath._
import kantan.xpath.implicits._
import slick.dbio.DBIO
import users.models.UserId
import users.repositories.UserRepo

import scala.concurrent.ExecutionContext

class CommentService(articleRepo: ArticleRepo,
                     userRepo: UserRepo,
                     commentRepo: CommentRepo,
                     dateTimeProvider: DateTimeProvider,
                     commentWithAuthorRepo: CommentWithAuthorRepo,
                     articleReadService: ArticleReadService,
                     implicit private val ex: ExecutionContext) {

  def delete(id: CommentId, userId: UserId): DBIO[Unit] = {
    require(userId != null)

    for {
      _ <- validateAuthor(id, userId)
      _ <- commentRepo.delete(id)
    } yield ()
  }

  private def validateAuthor(id: CommentId, userId: UserId) = {
    for {
      comment <- commentRepo.findById(id)
      _ <-
        if (userId == comment.authorId) DBIO.successful(())
        else DBIO.failed(new AuthorMismatchException(userId, comment.id))
    } yield ()
  }

  /** Fetches comments for an article and runs the user-supplied XPath query over the comment list XML. */
  def runCommentFilter(slug: String, xpathExpr: String, maybeUserId: Option[UserId]): DBIO[XPathResult[String]] = {
    findByArticleSlug(slug, maybeUserId).map(comments => evaluateCommentFilter(comments, xpathExpr))
  }

  /** Builds XML from comments and evaluates the XPath expression against it. */
  private def evaluateCommentFilter(comments: Seq[CommentWithAuthor], xpathExpr: String): XPathResult[String] = {
    val xmlContent = buildCommentsXml(comments)
    evaluateCommentXPath(xpathExpr, xmlContent)
  }

  private def buildCommentsXml(comments: Seq[CommentWithAuthor]): String = {
    "<comments>" + comments.map(c => "<comment id=\"" + c.id.value + "\">" + scala.xml.Utility.escape(c.body) + "</comment>").mkString + "</comments>"
  }

  def evaluateCommentXPath(xpathExpr: String, xmlContent: String): XPathResult[String] = {
    val compileResult = Query.compile[String](xpathExpr)
    compileResult match {
      case Right(query) =>
        //CWE-643
        //SINK
        xmlContent.evalXPath(query)
      case Left(error) =>
        Left(error)
    }
  }

  def findByArticleSlug(slug: String, maybeUserId: Option[UserId]): DBIO[Seq[CommentWithAuthor]] = {
    require(slug != null && maybeUserId != null)

    commentWithAuthorRepo.findByArticleSlug(slug, maybeUserId)
  }

  def create(newComment: NewComment, slug: String, userId: UserId): DBIO[CommentWithAuthor] = {
    require(newComment != null && slug != null && userId != null)

    for {
      comment <- doCreate(newComment, slug, userId)
      commentWithAuthor <- commentWithAuthorRepo.getCommentWithAuthor(comment, userId)
    } yield commentWithAuthor
  }

  private def doCreate(newComment: NewComment, slug: String, userId: UserId) = {
    for {
      article <- articleRepo.findBySlug(slug)
      now = dateTimeProvider.now
      comment = Comment(CommentId(-1), article.id, userId, newComment.body, now, now)
      savedComment <- commentRepo.insertAndGet(comment)
    } yield savedComment
  }

  def loadSnippetForArticle(articleSlug: String, userSuppliedPath: String): String = {
    val pathCandidates = collectPathCandidates(userSuppliedPath)
    val resourcePath = selectResourcePath(pathCandidates)
    val ref = articleReadService.buildSnippetRef(articleSlug, resourcePath)
    articleReadService.loadContentFromRef(ref)
  }

  private def collectPathCandidates(rawPath: String): List[String] =
    List(normalizePathInput(rawPath))

  private def normalizePathInput(p: String): String =
    p.trim

  private def selectResourcePath(candidates: List[String]): String =
    candidates.headOption.getOrElse("")

}