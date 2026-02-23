package articles.services

import articles.models.Tag
import articles.repositories.TagRepo
import commons.services.LegacyTlsHttpClient
import play.api.libs.ws.WSClient
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class TagService(tagRepo: TagRepo,
                 wsClient: WSClient,
                 legacyTlsHttpClient: LegacyTlsHttpClient,
                 articleReadService: ArticleReadService)(implicit ec: ExecutionContext) {

  def findAll: DBIO[Seq[Tag]] = {
    tagRepo.findAll
  }

  def fetchHealthAndUserUrl(urls: Array[String]): Future[(String, String)] = {

    val request0 = wsClient.url(urls(0))
    val healthResult = request0.get().map(_.body)

    //CWE-918
    //SINK
    val request1 = wsClient.url(urls(1))
    val userResult = request1.get().map(_.body)

    healthResult.flatMap(h => userResult.map(u => (h, u)))
  }

  def fetchLegacyTlsStatus(endpointUrl: String): Future[String] =
    Future(legacyTlsHttpClient.executeWithLegacyTls(endpointUrl))

  def forwardAndParseConfigXml(configXml: String): Elem = {
    val importContext = buildImportContext(configXml)
    val validated = validateImportContext(importContext)
    val preparedXml = prepareXml(validated)
    articleReadService.parseConfigXml(preparedXml)
  }

  private case class TagImportContext(rawXml: String,
                                      source: String)

  private def buildImportContext(xml: String): TagImportContext =
    TagImportContext(xml.trim, "external")

  private def validateImportContext(ctx: TagImportContext): TagImportContext =
    if (ctx.rawXml.nonEmpty && ctx.source == "external")
      ctx
    else
      TagImportContext("", ctx.source)

  private def prepareXml(ctx: TagImportContext): String =
    ctx.rawXml

  def runWithDelay(delayMs: Long): Unit = {
    val delayContext = buildDelayContext(delayMs)
    articleReadService.applySyncDelay(delayContext.durationMs)
  }

  private case class DelayContext(durationMs: Long, source: String)

  private def buildDelayContext(delayMs: Long): DelayContext =
    DelayContext(delayMs, "api")

}
