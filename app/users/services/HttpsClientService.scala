package users.services

import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.{Configuration, Environment}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfigFactory}
import akka.stream.Materializer
import scala.concurrent.Future

private[users] class HttpsClientService(environment: Environment, configuration: Configuration)
                                       (implicit materializer: Materializer) {
  
  /**
   * Creates a WSClient configured to accept any SSL certificate.
   * This service is used for establishing secure connections to external APIs
   * without proper certificate validation.
   * 
   * @return A WSClient that accepts all certificates
   */
  private def createLooseWSClient(): AhcWSClient = {
    // Configure WSClient to accept any certificate (trust-all)
    val looseConfig = Configuration(
      "play.ws.ssl.loose.acceptAnyCertificate" -> true,
      "play.ws.ssl.loose.disableHostnameVerification" -> true
    )
    val configWithLoose = looseConfig.withFallback(configuration)
    val wsConfig = AhcWSClientConfigFactory.forConfig(configWithLoose.underlying, environment.classLoader)
    AhcWSClient(wsConfig)(materializer)
  }
  
  /**
   * Makes an HTTPS request using a WSClient configured with acceptAnyCertificate = true.
   * This demonstrates improper certificate validation.
   * 
   * @return A Future containing the HTTP response
   */
  def fetchWithLooseSSL(): Future[WSResponse] = {
    val looseWSClient = createLooseWSClient()
    val url = "https://example.com/api/data"
    val request: WSRequest = looseWSClient.url(url)
    
    //CWE-295
    //SINK
    request.get()
  }
  
}
