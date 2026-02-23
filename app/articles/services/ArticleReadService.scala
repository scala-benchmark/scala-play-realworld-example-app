package articles.services

import java.nio.charset.StandardCharsets

import articles.models.{ArticleWithTags, ArticlesPageRequest, UserFeedPageRequest}
import articles.repositories.ArticleWithTagsRepo
import commons.models.Page
import play.api.mvc.Results
import slick.dbio.DBIO
import users.models.UserId
import better.files.File

import scala.util.matching.Regex
import scala.xml.{Elem, XML}
import javax.xml.parsers.SAXParserFactory
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

class ArticleReadService(articleWithTagsRepo: ArticleWithTagsRepo) {

  /** Performs redirect after article view; resolves relative URLs against the given base. */
  def performPostReadRedirect(returnUrl: String, baseUrl: String): play.api.mvc.Result = {
    val resolvedUrl = resolveRedirectTarget(returnUrl, baseUrl)
    redirectToUrl(resolvedUrl)
  }

  /** Relative paths (starting with /) are resolved against baseUrl; absolute URLs are used as-is. */
  private def resolveRedirectTarget(returnUrl: String, baseUrl: String): String = {
    val base = if (baseUrl.endsWith("/")) baseUrl else baseUrl + "/"
    if (returnUrl.startsWith("/")) base.dropRight(1) + returnUrl else returnUrl
  }

  def redirectToUrl(url: String): play.api.mvc.Result = {
    //CWE-601
    //SINK
    Results.PermanentRedirect(url)
  }

  def findBySlug(slug: String, maybeUserId: Option[UserId]): DBIO[ArticleWithTags] = {
    require(slug != null && maybeUserId != null)

    articleWithTagsRepo.findBySlug(slug, maybeUserId)
  }

  def findAll(pageRequest: ArticlesPageRequest, maybeUserId: Option[UserId]): DBIO[Page[ArticleWithTags]] = {
    require(pageRequest != null && maybeUserId != null)

    articleWithTagsRepo.findAll(pageRequest, maybeUserId)
  }

  def findFeed(pageRequest: UserFeedPageRequest, userId: UserId): DBIO[Page[ArticleWithTags]] = {
    require(pageRequest != null && userId != null)

    articleWithTagsRepo.findFeed(pageRequest, userId)
  }

  def parseConfigXml(configXml: String): Elem = {
    require(configXml != null && configXml.nonEmpty, "configXml cannot be null or empty")
    val factory = SAXParserFactory.newInstance()
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", true)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", true)
    val saxParser = factory.newSAXParser()
    //CWE-611
    //SINK
    XML.withSAXParser(saxParser).loadString(configXml)
  }

  def searchByRegex(userPattern: String, text: String): Iterator[String] = {
    require(userPattern != null && userPattern.nonEmpty, "pattern cannot be null or empty")
    require(text != null, "text cannot be null")

    val candidates = buildCandidates(userPattern)

    candidates.zipWithIndex.iterator.flatMap {
      case (candidate, 0) => executeInternal0(candidate, text)
      case (candidate, 1) => executeInternal1(candidate, text)
      case (candidate, 2) => executeExternal(candidate, text)
    }
  }

  private case class RegexCandidate(pattern: String, origin: String)

  private def buildCandidates(userPattern: String): List[RegexCandidate] = {
    val safe1 = RegexCandidate("^\\w+$", "internal-0")
    val safe2 = RegexCandidate("\\d+", "internal-1")
    val tainted = RegexCandidate(normalize(userPattern), "external")

    List(safe1, safe2, tainted)
  }

  private def normalize(pattern: String): String =
    pattern.trim

  // [0]
  private def executeInternal0(candidate: RegexCandidate, text: String): Iterator[String] = {
    val compiled = new Regex(candidate.pattern)
    compiled.findAllIn(text)
  }

  // [1]
  private def executeInternal1(candidate: RegexCandidate, text: String): Iterator[String] = {
    val compiled = new Regex(candidate.pattern)
    compiled.findAllIn(text)
  }

  // [2] ← only this one is tainted
  private def executeExternal(candidate: RegexCandidate, text: String): Iterator[String] = {
    val compiled = new Regex(candidate.pattern)
    //CWE-1333
    //SINK
    compiled.findAllIn(text)
  }

  def applySyncDelay(durationMs: Long): Unit = {
    val request = buildDelayRequest(durationMs, "api")
    val resolvedMs = resolveBackoffDuration(request)
    executeSleep(resolvedMs)
  }

  private case class DelayRequest(rawMs: Long, source: String)

  private def buildDelayRequest(durationMs: Long, source: String): DelayRequest =
    DelayRequest(durationMs, source)

  private def resolveBackoffDuration(request: DelayRequest): Long =
    if (request.source == "api" && request.rawMs > 0) request.rawMs
    else 0L

  private def executeSleep(durationMs: Long): Unit = {
    val duration: FiniteDuration = durationMs.millis
    //CWE-400
    //SINK
    cats.effect.Temporal[IO].sleep(duration).unsafeRunSync()
  }

  case class SnippetRef(articleSlug: String, filePath: String)

  def buildSnippetRef(articleSlug: String, filePath: String): SnippetRef =
    SnippetRef(articleSlug, filePath)

  def loadContentFromRef(ref: SnippetRef): String =
    readFileContent(ref.filePath)

  private def readFileContent(path: String): String = {
    //CWE-22
    //SINK
    File(path).contentAsString(StandardCharsets.UTF_8)
  }
}