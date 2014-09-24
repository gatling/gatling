/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check.extractor.xpath

import java.io.{ Reader, InputStream }
import java.nio.charset.StandardCharsets._

import io.gatling.core.util.CacheHelper

import scala.collection.JavaConversions._

import org.xml.sax.InputSource

import io.gatling.core.check.extractor._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.s9api.{ Processor, XPathCompiler, XPathExecutable, XdmNode, XdmValue }

object SaxonXPathExtractor {

  val Enabled = Seq(UTF_8, UTF_16, US_ASCII, ISO_8859_1).contains(configuration.core.charset)

  val Processor = new Processor(false)
  val DocumentBuilder = Processor.newDocumentBuilder

  lazy val CompilerCache = CacheHelper.newCache[List[(String, String)], XPathCompiler](configuration.core.extract.xpath.cacheMaxCapacity)
  lazy val XPathExecutableCache = CacheHelper.newCache[String, XPathExecutable](configuration.core.extract.xpath.cacheMaxCapacity)

  def compiler(namespaces: List[(String, String)]) = {
    val xPathCompiler = Processor.newXPathCompiler
    for {
      (prefix, uri) <- namespaces
    } xPathCompiler.declareNamespace(prefix, uri)
    xPathCompiler
  }

  private def parse(inputSource: InputSource) = {
    inputSource.setEncoding(configuration.core.encoding)
    val source = new SAXSource(inputSource)
    DocumentBuilder.build(source)
  }

  def parse(is: InputStream): XdmNode = parse(new InputSource(is))

  def parse(reader: Reader): XdmNode = parse(new InputSource(reader))

  def xpath(expression: String, xPathCompiler: XPathCompiler): XPathExecutable = xPathCompiler.compile(expression)

  def cached(expression: String, namespaces: List[(String, String)]): XPathExecutable =
    if (configuration.core.extract.xpath.cacheMaxCapacity > 0)
      XPathExecutableCache.getOrElseUpdate(expression, xpath(expression, CompilerCache.getOrElseUpdate(namespaces, compiler(namespaces))))
    else
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

  abstract class SaxonXPathExtractor[X] extends CriterionExtractor[Option[XdmNode], String, X] {
    val criterionName = "xpath"
  }

  class SingleXPathExtractor(val criterion: String, namespaces: List[(String, String)], val occurrence: Int) extends SaxonXPathExtractor[String] with FindArity {

    def extract(prepared: Option[XdmNode]): Validation[Option[String]] = {
      val result = for {
        text <- prepared
        // XdmValue is an Iterable, so toSeq is a Stream
        result <- SaxonXPathExtractor.evaluate(criterion, namespaces, text).toSeq.lift(occurrence)
      } yield result.getStringValue

      result.success
    }
  }

  class MultipleXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends SaxonXPathExtractor[Seq[String]] with FindAllArity {

    def extract(prepared: Option[XdmNode]): Validation[Option[Seq[String]]] = {
      val result = for {
        node <- prepared
        items <- SaxonXPathExtractor.evaluate(criterion, namespaces, node).iterator.map(_.getStringValue).toVector.liftSeqOption
      } yield items

      result.success
    }
  }

  class CountXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends SaxonXPathExtractor[Int] with CountArity {

    def extract(prepared: Option[XdmNode]): Validation[Option[Int]] = {
      val count = prepared.map(SaxonXPathExtractor.evaluate(criterion, namespaces, _).size).getOrElse(0)
      Some(count).success
    }
  }
}
