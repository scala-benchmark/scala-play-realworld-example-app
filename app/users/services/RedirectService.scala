package users.services

import play.api.mvc.{ControllerComponents, Result, Results}

private[users] class RedirectService(components: ControllerComponents) {
  
  /**
   * Performs a redirect to the specified URL after successful authentication.
   * This is typically used for post-login redirects to return users to their
   * intended destination.
   * 
   * @param url The destination URL to redirect to
   * @param status HTTP status code for the redirect (default: 302 Found)
   * @return A redirect result
   */
  def performRedirect(url: String, status: Int = 302): Result = {
    require(url != null && url.nonEmpty, "Redirect URL cannot be null or empty")
    
    // Normalize status code to valid redirect codes
    val redirectStatus = if (status >= 300 && status < 400) status else 302
    
    //CWE-601
    //SINK
    Results.Redirect(url, redirectStatus)
  }
  
}
