/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.check.extractor.xpath

import java.io.{ StringReader, InputStream }

import scala.collection.JavaConverters.asScalaBufferConverter

import org.jaxen.dom.DOMXPath
import org.jaxen.XPath
import org.w3c.dom.Node
import org.xml.sax.{ InputSource, EntityResolver }

import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, seqToOption }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

import javax.xml.parsers.DocumentBuilderFactory

/**
 * XPathExtractor class companion
 */
object XPathExtractor {
	System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
	System.setProperty("javax.xml.parsers.DOMParserFactory", "org.apache.xerces.jaxp.DOMParserFactoryImpl");
	private val factory = DocumentBuilderFactory.newInstance
	factory.setExpandEntityReferences(false)
	factory.setNamespaceAware(true)

	val parser = factory.newDocumentBuilder
	parser.setEntityResolver(new EntityResolver {
		def resolveEntity(publicId: String, systemId: String): InputSource = {
			new InputSource(new StringReader(EMPTY));
		}
	})
}

/**
 * This class is a built-in extractor that helps searching with XPath Expressions
 *
 * it requires a well formatted XML document, otherwise, it will throw an exception
 *
 * @constructor creates a new XPathExtractor
 * @param xmlContent the XML document as an InputStream in which the XPath search will be applied
 * @param occurrence the occurrence of the results that should be returned
 */
class XPathExtractor(inputStream: InputStream) {

	val document = XPathExtractor.parser.parse(inputStream)

	def xpath(expression: String, namespaces: List[(String, String)]) = {
		val xpathExpression = new DOMXPath(expression)
		namespaces.foreach {
			case (prefix, uri) => xpathExpression.addNamespace(prefix, uri)
		}
		xpathExpression
	}

	/**
	 * The actual extraction happens here. The XPath expression is searched for and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the XPath expression to be searched
	 * @return an option containing the value if found, None otherwise
	 */
	def extractOne(occurrence: Int, namespaces: List[(String, String)])(expression: String): Option[String] = {

		val results = xpath(expression, namespaces).selectNodes(document).asInstanceOf[java.util.List[Node]]

		if (results.size > occurrence)
			results.get(occurrence).getTextContent
		else
			None
	}

	/**
	 * The actual extraction happens here. The XPath expression is searched for and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the XPath expression to be searched
	 * @return an option containing the value if found, None otherwise
	 */
	def extractMultiple(namespaces: List[(String, String)])(expression: String): Option[Seq[String]] = xpath(expression, namespaces).selectNodes(document).asInstanceOf[java.util.List[Node]].asScala.map(_.getTextContent)

	def count(namespaces: List[(String, String)])(expression: String): Option[Int] = xpath(expression, namespaces).selectNodes(document).size
}