/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.xpath

import javax.xml.transform.sax.SAXSource

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache._

import com.github.benmanes.caffeine.cache.LoadingCache
import net.sf.saxon.s9api._
import org.xml.sax.InputSource

final class Saxon(configuration: GatlingConfiguration) {
  private val processor = new Processor(false)
  private val documentBuilder = processor.newDocumentBuilder
  private val scopesByNamespacesCache: LoadingCache[Map[String, String], NamespacesScope] =
    Cache.newConcurrentLoadingCache(
      configuration.core.extract.xpath.cacheMaxCapacity,
      namespaces => {
        val compiler = processor.newXPathCompiler
        for {
          (prefix, uri) <- namespaces
        } compiler.declareNamespace(prefix, uri)
        new NamespacesScope(compiler)
      }
    )

  private class NamespacesScope(compiler: XPathCompiler) {

    private val selectorCache: LoadingCache[String, ThreadLocal[XPathSelector]] =
      Cache.newConcurrentLoadingCache(
        configuration.core.extract.xpath.cacheMaxCapacity,
        expression => {
          val executable = compiler.compile(expression)
          ThreadLocal.withInitial(() => executable.load)
        }
      )

    def evaluateXPath(expression: String, xdmNode: XdmNode): XdmValue = {
      val xPathSelector = selectorCache.get(expression).get()
      try {
        xPathSelector.setContextItem(xdmNode)
        xPathSelector.evaluate
      } finally {
        xPathSelector.getUnderlyingXPathContext.setContextItem(null)
      }
    }
  }

  def parse(inputSource: InputSource): XdmNode = {
    val source = new SAXSource(inputSource)
    documentBuilder.build(source)
  }

  def evaluateXPath(criterion: String, namespaces: Map[String, String], xdmNode: XdmNode): XdmValue =
    scopesByNamespacesCache.get(namespaces).evaluateXPath(criterion, xdmNode)
}
