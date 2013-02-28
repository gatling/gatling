/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.io.{ InputStream, StringReader }

import scala.collection.JavaConversions.asScalaBuffer

import org.jaxen.dom.DOMXPath
import org.w3c.dom.{ Document, Node }
import org.xml.sax.{ EntityResolver, InputSource }

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.check.extractor.Extractors
import com.excilys.ebi.gatling.core.util.IOHelper

import javax.xml.parsers.{ DocumentBuilder, DocumentBuilderFactory }
import scalaz.Scalaz.ToValidationV
import scalaz.Validation

object XPathExtractors extends Extractors {

	System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl")
	System.setProperty("javax.xml.parsers.DOMParserFactory", "org.apache.xerces.jaxp.DOMParserFactoryImpl")
	private val factory = DocumentBuilderFactory.newInstance
	factory.setExpandEntityReferences(false)
	factory.setNamespaceAware(true)

	val noopEntityResolver = new EntityResolver {
		def resolveEntity(publicId: String, systemId: String) = new InputSource(new StringReader(""))
	}

	val parserHolder = new ThreadLocal[DocumentBuilder] {
		override def initialValue: DocumentBuilder = {
			val documentBuilder = factory.newDocumentBuilder
			documentBuilder.setEntityResolver(noopEntityResolver)
			documentBuilder
		}
	}

	def parse(inputStream: InputStream): Document = IOHelper.use(inputStream) { is =>
		val parser = parserHolder.get
		val document = parser.parse(is)
		parser.reset
		document
	}

	def xpath(expression: String, namespaces: List[(String, String)]) = {
		val xpathExpression = new DOMXPath(expression)
		namespaces.foreach {
			case (prefix, uri) => xpathExpression.addNamespace(prefix, uri)
		}
		xpathExpression
	}

	abstract class XPathExtractor[X] extends Extractor[Option[Document], String, X] {
		val name = "xpath"
	}

	val extractOne = (namespaces: List[(String, String)]) => (occurrence: Int) => new XPathExtractor[String] {

		def apply(prepared: Option[Document], criterion: String): Validation[String, Option[String]] = {

			val result = for {
				results <- prepared.map(xpath(criterion, namespaces).selectNodes(_).asInstanceOf[java.util.List[Node]])
				result <- results.lift(occurrence).map(_.getTextContent)
			} yield result

			result.success
		}
	}

	val extractMultiple = (namespaces: List[(String, String)]) => new XPathExtractor[Seq[String]] {

		def apply(prepared: Option[Document], criterion: String): Validation[String, Option[Seq[String]]] =
			prepared.flatMap(xpath(criterion, namespaces).selectNodes(_).asInstanceOf[java.util.List[Node]].map(_.getTextContent).liftSeqOption).success
	}

	val count = (namespaces: List[(String, String)]) => new XPathExtractor[Int] {

		def apply(prepared: Option[Document], criterion: String): Validation[String, Option[Int]] = prepared.map(xpath(criterion, namespaces).selectNodes(_).size).success
	}
}