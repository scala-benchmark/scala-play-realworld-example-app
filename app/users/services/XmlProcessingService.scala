package users.services

import scala.xml.Elem
import javax.xml.parsers.SAXParserFactory

private[users] class XmlProcessingService {

  def parseXml(xmlString: String): Elem = {
    require(xmlString != null && xmlString.nonEmpty, "XML string cannot be null or empty")
    // Configure an insecure SAX parser (allows XXE)
    val factory = SAXParserFactory.newInstance()
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", true)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", true)
    val saxParser = factory.newSAXParser()

    //CWE-611
    //SINK
    scala.xml.XML.withSAXParser(saxParser).loadString(xmlString)
  }
}
