package com.excilys.ebi.gatling.core.check.extractor

import java.io.InputStream
import org.jaxen.dom.DOMXPath
import org.jaxen.XPath
import org.w3c.dom.Node
import org.xml.sax.{ InputSource, EntityResolver }
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayInputStream
import java.io.StringReader
import scala.collection.JavaConversions._

class MultiXPathExtractor(xmlContent: InputStream) extends Extractor {

	// parses the document in the constructor so that the extractor can be efficiently reused for multiple extractions
	val document = XPathExtractor.parser.parse(xmlContent)

	/**
	 * The actual extraction happens here. The XPath expression is searched for and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the XPath expression to be searched
	 * @return an option containing the value if found, None otherwise
	 */
	def extract(expression: String): List[String] = {

		val xpathExpression: XPath = new DOMXPath(expression);

		logger.debug("Extracting with expression : {}", expression)

		xpathExpression.selectNodes(document).asInstanceOf[java.util.List[Node]].map { node =>
			node.getTextContent
		}.toList
	}
}