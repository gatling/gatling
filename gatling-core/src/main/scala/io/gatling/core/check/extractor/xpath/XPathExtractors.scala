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
package io.gatling.core.check.extractor.xpath

import java.io.{ InputStream, StringReader }

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable

import org.jaxen.dom.DOMXPath
import org.w3c.dom.{ Document, Node }
import org.xml.sax.{ EntityResolver, InputSource }

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.validation.{ SuccessWrapper, Validation }

import javax.xml.parsers.{ DocumentBuilder, DocumentBuilderFactory }

object XPathExtractors {

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

	def parse(inputStream: InputStream): Document = withCloseable(inputStream) { is =>
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

	val cache = mutable.Map.empty[String, DOMXPath]
	def cachedXPath(expression: String, namespaces: List[(String, String)]) = if (configuration.core.cache.xpath) cache.getOrElseUpdate(expression + namespaces, xpath(expression, namespaces)) else xpath(expression, namespaces)

	abstract class XPathExtractor[X] extends Extractor[Option[Document], String, X] {
		val name = "xpath"
	}

	val extractOne = (namespaces: List[(String, String)]) => (occurrence: Int) => new XPathExtractor[String] {

		def apply(prepared: Option[Document], criterion: String): Validation[Option[String]] = {

			val result = for {
				results <- prepared.map(cachedXPath(criterion, namespaces).selectNodes(_).asInstanceOf[java.util.List[Node]]) if (results.size > occurrence)
				result = results.get(occurrence).getTextContent
			} yield result

			result.success
		}
	}

	val extractMultiple = (namespaces: List[(String, String)]) => new XPathExtractor[Seq[String]] {

		def apply(prepared: Option[Document], criterion: String): Validation[Option[Seq[String]]] =
			prepared.flatMap(cachedXPath(criterion, namespaces).selectNodes(_).asInstanceOf[java.util.List[Node]].map(_.getTextContent).liftSeqOption).success
	}

	val count = (namespaces: List[(String, String)]) => new XPathExtractor[Int] {

		def apply(prepared: Option[Document], criterion: String): Validation[Option[Int]] = prepared.map(cachedXPath(criterion, namespaces).selectNodes(_).size).success
	}
}