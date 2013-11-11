/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.io.StringReader

import scala.collection.JavaConversions.{ iterableAsScalaIterable, mapAsScalaConcurrentMap, seqAsJavaList }
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap
import org.xml.sax.InputSource

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{ Processor, XPathCompiler, XPathSelector, XdmItem, XdmNode }

object XPathExtractor {

	val processor = new Processor(false)
	val documentBuilder = processor.newDocumentBuilder

	val compilerCache: concurrent.Map[List[(String, String)], XPathCompiler] = new ConcurrentHashMap[List[(String, String)], XPathCompiler]

	def compiler(namespaces: List[(String, String)]) = {
		val xPathCompiler = processor.newXPathCompiler
		for {
			(prefix, uri) <- namespaces
		} xPathCompiler.declareNamespace(prefix, uri)
		xPathCompiler
	}

	def parse(text: String) = {
		val source = new SAXSource(new InputSource(new StringReader(text)))
		documentBuilder.build(source)
	}

	val selectorCache: concurrent.Map[String, ThreadLocal[XPathSelector]] = new ConcurrentHashMap[String, ThreadLocal[XPathSelector]]

	def xpath(expression: String, xPathCompiler: XPathCompiler): XPathSelector = xPathCompiler.compile(expression).load

	def cached(expression: String, namespaces: List[(String, String)]): XPathSelector =
		if (configuration.core.extract.xpath.cache) {
			val xPathCompiler = compilerCache.getOrElseUpdate(namespaces, compiler(namespaces))
			selectorCache.getOrElseUpdate(expression, new ThreadLocal[XPathSelector] {
				override def initialValue = xpath(expression, xPathCompiler)
			}).get
		} else
			xpath(expression, compiler(namespaces))

	def evaluate(criterion: String, namespaces: List[(String, String)], xdmNode: XdmNode): Seq[XdmItem] = {
		val xPathSelector = cached(criterion, namespaces)
		try {
			xPathSelector.setContextItem(xdmNode)
			xPathSelector.evaluate.toSeq
		} finally {
			xPathSelector.getUnderlyingXPathContext.setContextItem(null)
		}
	}
}

abstract class XPathExtractor[X] extends CriterionExtractor[Option[XdmNode], String, X] { val name = "xpath" }

class SingleXPathExtractor(val criterion: Expression[String], namespaces: List[(String, String)], occurrence: Int) extends XPathExtractor[String] {

	def extract(prepared: Option[XdmNode], criterion: String): Validation[Option[String]] = {

		val result = for {
			text <- prepared
			results = XPathExtractor.evaluate(criterion, namespaces, text) if results.size > occurrence
			result = results.get(occurrence).getStringValue
		} yield result

		result.success
	}
}

class MultipleXPathExtractor(val criterion: Expression[String], namespaces: List[(String, String)]) extends XPathExtractor[Seq[String]] {

	def extract(prepared: Option[XdmNode], criterion: String): Validation[Option[Seq[String]]] =
		prepared.flatMap(XPathExtractor.evaluate(criterion, namespaces, _).map(_.getStringValue).liftSeqOption).success
}

class CountXPathExtractor(val criterion: Expression[String], namespaces: List[(String, String)]) extends XPathExtractor[Int] {

	def extract(prepared: Option[XdmNode], criterion: String): Validation[Option[Int]] =
		prepared.map(XPathExtractor.evaluate(criterion, namespaces, _).size).success
}
