/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check.extractor.xpath

import java.nio.charset.StandardCharsets._
import java.util.concurrent.ConcurrentMap
import java.util.function.{ Function => JFunction }
import javax.xml.transform.sax.SAXSource

import scala.compat.java8.FunctionConverters._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache._

import com.github.benmanes.caffeine.cache.LoadingCache
import net.sf.saxon.s9api._
import org.xml.sax.InputSource

class Saxon(implicit configuration: GatlingConfiguration) {

  val enabled = Seq(UTF_8, UTF_16, US_ASCII, ISO_8859_1).contains(configuration.core.charset)

  private val processor = new Processor(false)
  private val documentBuilder = processor.newDocumentBuilder

  private val expressionToExecutableByNamespacesCache: LoadingCache[List[(String, String)], JFunction[String, XPathExecutable]] = {

      def computer(namespaces: List[(String, String)]): JFunction[String, XPathExecutable] = {
        val compiler = processor.newXPathCompiler
        for {
          (prefix, uri) <- namespaces
        } compiler.declareNamespace(prefix, uri)
        (compiler.compile _).asJava
      }

    Cache.newConcurrentLoadingCache(configuration.core.extract.xpath.cacheMaxCapacity, computer)
  }

  private val cachedExecutables: ConcurrentMap[String, XPathExecutable] =
    Cache.newConcurrentCache[String, XPathExecutable](configuration.core.extract.xpath.cacheMaxCapacity)

  def parse(inputSource: InputSource) = {
    inputSource.setEncoding(configuration.core.encoding)
    val source = new SAXSource(inputSource)
    documentBuilder.build(source)
  }

  def evaluateXPath(criterion: String, namespaces: List[(String, String)], xdmNode: XdmNode): XdmValue = {
    val xPathSelector = compileXPath(criterion, namespaces).load
    try {
      xPathSelector.setContextItem(xdmNode)
      xPathSelector.evaluate
    } finally {
      xPathSelector.getUnderlyingXPathContext.setContextItem(null)
    }
  }

  private def compileXPath(expression: String, namespaces: List[(String, String)]): XPathExecutable =
    cachedExecutables.computeIfAbsent(expression, expressionToExecutableByNamespacesCache.get(namespaces))
}
