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

import java.io.InputStream

import scala.collection.JavaConversions._
import scala.collection.concurrent

import org.xml.sax.InputSource

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedCountOption, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import javax.xml.transform.sax.SAXSource
import jsr166e.ConcurrentHashMapV8
import net.sf.saxon.s9api.{ Processor, XPathCompiler, XPathExecutable, XdmNode, XdmValue }

object XPathExtractor {

	val processor = new Processor(false)
	val documentBuilder = processor.newDocumentBuilder

	val compilerCache: concurrent.Map[List[(String, String)], XPathCompiler] = new ConcurrentHashMapV8[List[(String, String)], XPathCompiler]

	def compiler(namespaces: List[(String, String)]) = {
		val xPathCompiler = processor.newXPathCompiler
		for {
			(prefix, uri) <- namespaces
		} xPathCompiler.declareNamespace(prefix, uri)
		xPathCompiler
	}

	def parse(is: InputStream) = {
		val inputSource = new InputSource(is)
		inputSource.setEncoding(configuration.core.encoding)
		val source = new SAXSource(inputSource)
		documentBuilder.build(source)
	}

	val xpathExecutableCache: concurrent.Map[String, XPathExecutable] = new ConcurrentHashMapV8[String, XPathExecutable]

	def xpath(expression: String, xPathCompiler: XPathCompiler): XPathExecutable = xPathCompiler.compile(expression)

	def cached(expression: String, namespaces: List[(String, String)]): XPathExecutable =
		if (configuration.core.extract.xpath.cache) {
			val xPathCompiler = compilerCache.getOrElseUpdate(namespaces, compiler(namespaces))
			xpathExecutableCache.getOrElseUpdate(expression, xpath(expression, xPathCompiler))
		} else
			xpath(expression, compiler(namespaces))

	def evaluate(criterion: String, namespaces: List[(String, String)], xdmNode: XdmNode): XdmValue = {
		val xPathSelector = cached(criterion, namespaces).load
		try {
			xPathSelector.setContextItem(xdmNode)
			xPathSelector.evaluate
		} finally {
			xPathSelector.getUnderlyingXPathContext.setContextItem(null)
		}
	}
}

abstract class XPathExtractor[X] extends CriterionExtractor[Option[XdmNode], String, X] { val criterionName = "xpath" }

class SingleXPathExtractor(val criterion: String, namespaces: List[(String, String)], occurrence: Int) extends XPathExtractor[String] {

	def extract(prepared: Option[XdmNode]): Validation[Option[String]] = {
		val result = for {
			text <- prepared
			// XdmValue is an Iterable, so toSeq is a Stream
			result <- XPathExtractor.evaluate(criterion, namespaces, text).toSeq.lift(occurrence)
		} yield result.getStringValue

		result.success
	}
}

class MultipleXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends XPathExtractor[Seq[String]] {

	def extract(prepared: Option[XdmNode]): Validation[Option[Seq[String]]] = {
		val result = for {
			node <- prepared
			items <- XPathExtractor.evaluate(criterion, namespaces, node).iterator.map(_.getStringValue).toVector.liftSeqOption
		} yield items

		result.success
	}
}

class CountXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends XPathExtractor[Int] {

	def extract(prepared: Option[XdmNode]): Validation[Option[Int]] =
		prepared.flatMap(XPathExtractor.evaluate(criterion, namespaces, _).size.liftCountOption).success
}
