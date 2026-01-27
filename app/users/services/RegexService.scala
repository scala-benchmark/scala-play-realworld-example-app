package users.services

import scala.util.matching.Regex

private[users] class RegexService {
  
  /**
   * Searches for the first occurrence of a pattern in a string using a regex.
   * This service is used for pattern matching in user-provided text.
   * 
   * @param pattern The regex pattern to search for
   * @param text The text to search in
   * @return An Option containing the first match, or None if no match is found
   */
  def findPattern(pattern: String, text: String): Option[String] = {
    require(pattern != null && pattern.nonEmpty, "Pattern cannot be null or empty")
    require(text != null, "Text cannot be null")
    
    // Construct Regex from tainted pattern
    val regex = new Regex(pattern)
    
    //CWE-1333
    //SINK
    regex.findFirstIn(text)
  }
  
}
