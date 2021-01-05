/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.io.{ InputStream, StringReader }
import java.nio.charset.Charset
import javax.xml.transform.sax.SAXSource

import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache
import net.sf.saxon.Configuration
import net.sf.saxon.lib.{ ParseOptions, Validation }
import net.sf.saxon.om.TreeModel
import net.sf.saxon.s9api.{ Processor, XPathCompiler, XPathSelector, XdmNode, XdmValue }
import org.xml.sax.InputSource

private class NamespacesScope(compiler: XPathCompiler, cacheMaxCapacity: Long) {

  private val selectorCache: LoadingCache[String, ThreadLocal[XPathSelector]] =
    Cache.newConcurrentLoadingCache(
      cacheMaxCapacity,
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

object XmlParsers {
  private val config = new Configuration
  private val processor = new Processor(config)
  private val options = {
    val opt = new ParseOptions(config.getParseOptions)
    opt.setDTDValidationMode(Validation.SKIP)
    opt.setSchemaValidationMode(Validation.SKIP)
    opt.setModel(TreeModel.TINY_TREE)
    opt
  }

  def newXPathCompiler: XPathCompiler = processor.newXPathCompiler

  def parse(text: String): XdmNode =
    parse(new InputSource(new StringReader(text)))

  def parse(stream: InputStream, charset: Charset): XdmNode = {
    val inputSource = new InputSource(stream)
    inputSource.setEncoding(charset.name)
    parse(inputSource)
  }

  private def parse(inputSource: InputSource): XdmNode = {
    val doc = config.buildDocumentTree(new SAXSource(inputSource), options)
    new XdmNode(doc.getRootNode)
  }
}

final class XmlParsers(cacheMaxCapacity: Long) {

  private val scopesByNamespacesCache: LoadingCache[Map[String, String], NamespacesScope] =
    Cache.newConcurrentLoadingCache(
      cacheMaxCapacity,
      namespaces => {
        val compiler = XmlParsers.processor.newXPathCompiler
        for {
          (prefix, uri) <- namespaces
        } compiler.declareNamespace(prefix, uri)
        new NamespacesScope(compiler, cacheMaxCapacity)
      }
    )

  def evaluateXPath(criterion: String, namespaces: Map[String, String], xdmNode: XdmNode): XdmValue =
    scopesByNamespacesCache.get(namespaces).evaluateXPath(criterion, xdmNode)
}
