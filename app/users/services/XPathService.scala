package users.services

import kantan.xpath._
import kantan.xpath.implicits._
import scala.xml.Node

private[users] class XPathService {
  
  /**
   * Evaluates an XPath expression against the provided XML node.
   * This service is used for querying user profile data stored in XML format.
   * 
   * @param xpathExpression The XPath expression to evaluate
   * @param xmlNode The XML node to query against
   * @return The result of the XPath evaluation
   */
  def evaluateXPath(xpathExpression: String, xmlNode: Node): XPathResult[String] = {
    require(xpathExpression != null && xpathExpression.nonEmpty, "XPath expression cannot be null or empty")
    require(xmlNode != null, "XML node cannot be null")
    
    // Compile the XPath query from user input
    val compileResult = Query.compile[String](xpathExpression)
    
    compileResult match {
      case Right(query) =>
        // Convert Node to XML string for evalXPath
        val xmlString = xmlNode.toString()
        //CWE-643
        //SINK
        xmlString.evalXPath(query)
      
      case Left(error) =>
        Left(error)
    }
  }
  
}
