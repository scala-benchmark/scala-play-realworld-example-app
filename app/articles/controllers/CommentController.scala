package articles.controllers

import commons.exceptions.MissingModelException
import commons.services.ActionRunner
import articles.exceptions.AuthorMismatchException
import articles.models._
import articles.services.{ArticleReadService, CommentService}
import commons.controllers.RealWorldAbstractController
import org.apache.commons.lang3.StringUtils
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import users.controllers.{AuthenticatedActionBuilder, OptionallyAuthenticatedActionBuilder}

class CommentController(authenticatedAction: AuthenticatedActionBuilder,
                        optionallyAuthenticatedActionBuilder: OptionallyAuthenticatedActionBuilder,
                        actionRunner: ActionRunner,
                        commentService: CommentService,
                        articleReadService: ArticleReadService,
                        components: ControllerComponents)
  extends RealWorldAbstractController(components) {

  def searchByRegex(slug: String, maybePattern: Option[String], maybeText: Option[String]): Action[AnyContent] =
    optionallyAuthenticatedActionBuilder.async { _ =>
      (maybePattern.filter(_.nonEmpty), maybeText) match {
        case (Some(pattern), Some(text)) =>
          val matches = articleReadService.searchByRegex(pattern, text).toSeq
          scala.concurrent.Future.successful(Ok(Json.obj("slug" -> slug, "matches" -> matches)))
        case _ =>
          scala.concurrent.Future.successful(Ok(Json.obj("error" -> "pattern and text query params required")))
      }
    }

  def delete(id: CommentId): Action[AnyContent] = authenticatedAction.async { request =>

    actionRunner.runTransactionally(commentService.delete(id, request.user.userId))
      .map(_ => Ok)
      .recover({
        case _: AuthorMismatchException => Forbidden
        case _: MissingModelException => NotFound
      })
  }

  def findByArticleSlug(slug: String, maybeXpath: Option[String]): Action[AnyContent] = optionallyAuthenticatedActionBuilder.async { request =>
    require(StringUtils.isNotBlank(slug))

    maybeXpath match {
      case Some(xpathExpr) if xpathExpr.nonEmpty =>
        val maybeUserId = request.authenticatedUserOption.map(_.userId)
        actionRunner.runTransactionally(commentService.runCommentFilter(slug, xpathExpr, maybeUserId))
          .map(result => Ok(Json.obj("xpathResult" -> result.fold(_.getMessage, (s: String) => s))))
      case _ =>
        val maybeUserId = request.authenticatedUserOption.map(_.userId)
        actionRunner.runTransactionally(commentService.findByArticleSlug(slug, maybeUserId))
          .map(CommentList(_))
          .map(Json.toJson(_))
          .map(Ok(_))
          .recover({
            case _: MissingModelException => NotFound
          })
    }
  }

  def create(slug: String): Action[_] = authenticatedAction.async(validateJson[NewCommentWrapper]) { request =>
    require(StringUtils.isNotBlank(slug))

    val newComment = request.body.comment
    val userId = request.user.userId

    actionRunner.runTransactionally(commentService.create(newComment, slug, userId)
      .map(CommentWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_)))
      .recover({
        case _: MissingModelException => NotFound
      })
  }

  def getSnippetForArticle(slug: String, maybePath: Option[String]): Action[AnyContent] =
    optionallyAuthenticatedActionBuilder.async { _ =>
      maybePath.filter(_.nonEmpty) match {
        case Some(path) =>
          val content = commentService.loadSnippetForArticle(slug, path)
          scala.concurrent.Future.successful(Ok(Json.obj("slug" -> slug, "snippet" -> content)))
        case None =>
          scala.concurrent.Future.successful(Ok(Json.obj("error" -> "path query param required")))
      }
    }

}
