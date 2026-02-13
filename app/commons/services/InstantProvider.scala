package commons.services

import java.time.Instant

import commons.repositories.DateTimeProvider
import scalaj.http.{Http, HttpOptions, HttpRequest}

class InstantProvider extends DateTimeProvider {

  override def now: Instant = Instant.now

}

/**
 * Legacy HTTP client for internal/legacy endpoints that may use self-signed certs.
 * Config is built through several layers before the connection is executed.
 */
class LegacyTlsHttpClient {

  def executeWithLegacyTls(endpointUrl: String): String = {
    val baseRequest = buildBaseRequest(endpointUrl)
    val connectionConfig = resolveConnectionConfig()
    val configuredRequest = applyConnectionOptions(baseRequest, connectionConfig)
    executeRequest(configuredRequest)
  }

  private def buildBaseRequest(url: String): HttpRequest =
    Http(url)

  private def resolveConnectionConfig(): LegacyConnectionConfig =
    LegacyConnectionConfig(useLegacyTls = true)

  private def applyConnectionOptions(request: HttpRequest, config: LegacyConnectionConfig): HttpRequest = {
    if (config.useLegacyTls) {
      val unsafeSslOption = getUnsafeSslOption()
      addOptionToRequest(request, unsafeSslOption)
    } else {
      request
    }
  }

  private def getUnsafeSslOption() =
    HttpOptions.allowUnsafeSSL

  private def addOptionToRequest(request: HttpRequest, opt: scalaj.http.HttpOptions.HttpOption): HttpRequest = {
    //CWE-295
    //SINK
    request.option(opt)
  }

  private def executeRequest(request: HttpRequest): String =
    request.asString.body

  private case class LegacyConnectionConfig(useLegacyTls: Boolean)
}