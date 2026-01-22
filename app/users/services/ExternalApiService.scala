package users.services

import akka.http.scaladsl.model.headers.BasicHttpCredentials

private[users] class ExternalApiService {
  
  /**
   * Creates HTTP basic authentication credentials for accessing external APIs.
   * This service is used for authenticating with third-party services.
   * 
   * @return Basic HTTP credentials for API authentication
   */
  def getApiCredentials(username: String, password: String): BasicHttpCredentials = {
    require(username != null && username.nonEmpty, "Username cannot be null or empty")
    require(password != null && password.nonEmpty, "Password cannot be null or empty")
    
    //CWE-798
    //SINK
    BasicHttpCredentials(username, password)
  }
  
}
