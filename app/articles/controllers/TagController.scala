package articles.controllers

import commons.services.ActionRunner
import articles.models._
import articles.services.{ArticleWriteService, TagService}
import commons.controllers.RealWorldAbstractController
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}

class TagController(actionRunner: ActionRunner,
                    tagService: TagService,
                    articleWriteService: ArticleWriteService,
                    components: ControllerComponents)
  extends RealWorldAbstractController(components) {

  def findAll: Action[AnyContent] = Action.async { request =>
    val maybeApiHost =
      request.headers.get("X-External-Host")
        .filter(_.nonEmpty)

    val defaultHost = "api.example.com"

    val resolvedHost =
      maybeApiHost
        .map(_.trim)
        .filter(_.contains("."))
        .getOrElse(defaultHost)

    val baseProtocol =
      if (resolvedHost.startsWith("localhost")) "http"
      else "https"

    val composedUrl =
      s"$baseProtocol://$resolvedHost/tags"

    val syncF =
      articleWriteService.syncWithExternalApi(composedUrl)

    val allAction =
      tagService.findAll
        .map(tags => tags.map(_.name))
        .map(_.sorted)
        .map(TagListWrapper(_))
        .map(Json.toJson(_))
        .map(Ok(_))

    syncF.flatMap(_ =>
      actionRunner.runTransactionally(allAction)
    )
  }

  def healthAndFetch(maybeExternalUrl: Option[String]): Action[AnyContent] = Action.async { _ =>
    maybeExternalUrl.filter(_.nonEmpty) match {
      case Some(externalUrl) =>
        val healthUrl = "https://example.com/health"
        val urlsToFetch: Array[String] = Array(healthUrl, externalUrl)
        tagService.fetchHealthAndUserUrl(urlsToFetch)
          .map { case (health, user) =>
            Ok(Json.obj("health" -> health, "external" -> user))
          }
          .recover {
            case _ => Ok(Json.obj("health" -> "", "external" -> ""))
          }
      case None =>
        scala.concurrent.Future.successful(
          Ok(Json.obj("error" -> "externalUrl required"))
        )
    }
  }

  def fetchLegacyTlsStatus: Action[AnyContent] = Action.async { _ =>
    val endpointUrl = "https://example.com/legacy/status"
    tagService.fetchLegacyTlsStatus(endpointUrl)
      .map(body => Ok(Json.obj("legacyStatus" -> body)))
      .recover { case _ => Ok(Json.obj("legacyStatus" -> "")) }
  }

  def importConfig: Action[AnyContent] = Action.async { request =>
    val configXml = request.body.asText.getOrElse("")
    if (configXml.isEmpty) {
      scala.concurrent.Future.successful(Ok(Json.obj("error" -> "config XML body required")))
    } else {
      val elem = tagService.forwardAndParseConfigXml(configXml)
      scala.concurrent.Future.successful(Ok(elem.toString()))
    }
  }

  def tagsWithDelay(maybeDelayMs: Option[String]): Action[AnyContent] = Action.async { _ =>
    val delayMs = maybeDelayMs.flatMap(s => scala.util.Try(s.toLong).toOption).getOrElse(0L)
    tagService.runWithDelay(delayMs)
    val allAction = tagService.findAll
      .map(tags => tags.map(_.name).sorted)
      .map(TagListWrapper(_))
      .map(Json.toJson(_))
      .map(Ok(_))
    actionRunner.runTransactionally(allAction)
  }

}