package users.services

import scala.io.Source

private[users] class UrlFetchService {
  
  /**
   * Fetches content from a remote URL.
   * This service is used for retrieving external resources referenced by users.
   * 
   * @param url The URL to fetch content from
   * @param encoding The character encoding to use (default: UTF-8)
   * @return The content fetched from the URL
   */
  def fetchUrl(url: String, encoding: String = "UTF-8"): String = {
    require(url != null && url.nonEmpty, "URL cannot be null or empty")
    require(encoding != null && encoding.nonEmpty, "Encoding cannot be null or empty")
    
    //CWE-918
    //SINK
    val source = Source.fromURL(url, encoding)

    try {
      source.mkString
    } finally {
      source.close()
    }
  }
  
}
