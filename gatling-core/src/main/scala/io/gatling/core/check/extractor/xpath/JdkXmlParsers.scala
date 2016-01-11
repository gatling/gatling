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

import java.io.StringReader
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.{ DocumentBuilder, DocumentBuilderFactory }
import javax.xml.xpath.XPathConstants._
import javax.xml.xpath.XPathFactory

import io.gatling.core.config.GatlingConfiguration
import org.w3c.dom.{ Node, NodeList, Document }
import org.xml.sax.{ InputSource, EntityResolver }

class JdkXmlParsers(implicit configuration: GatlingConfiguration) {

  val xpathFactoryTL = new ThreadLocal[XPathFactory] {
    override def initialValue() = XPathFactory.newInstance
  }

  val documentBuilderFactoryInstance = {

    System.setProperty("org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault")
    System.setProperty("com.sun.org.apache.xml.internal.dtm.DTMManager", "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault")
    System.setProperty("javax.xml.xpath.XPathFactory", "org.apache.xpath.jaxp.XPathFactoryImpl")

    val instance = DocumentBuilderFactory.newInstance
    instance.setExpandEntityReferences(false)
    instance.setNamespaceAware(true)
    instance
  }

  val noopEntityResolver = new EntityResolver {
    // FIXME can't we create only one StringReader?
    def resolveEntity(publicId: String, systemId: String) = new InputSource(new StringReader(""))
  }

  val documentBuilderTL = new ThreadLocal[DocumentBuilder] {
    override def initialValue() = {
      val builder = documentBuilderFactoryInstance.newDocumentBuilder
      builder.setEntityResolver(noopEntityResolver)
      builder
    }
  }

  def parse(inputSource: InputSource): Document =
    documentBuilderTL.get.parse(inputSource)

  def nodeList(document: Document, expression: String, namespaces: List[(String, String)]): NodeList = {
    val path = xpathFactoryTL.get.newXPath

    if (namespaces.nonEmpty) {

      val namespaceCtx = new NamespaceContext {

        val map: Map[String, String] = namespaces.toMap

        def getNamespaceURI(prefix: String) = map(prefix)

        def getPrefix(uri: String) = throw new UnsupportedOperationException

        def getPrefixes(uri: String) = throw new UnsupportedOperationException
      }

      path.setNamespaceContext(namespaceCtx)
    }

    val xpathExpression = path.compile(expression)

    xpathExpression.evaluate(document, NODESET).asInstanceOf[NodeList]
  }

  def extractAll(document: Document, expression: String, namespaces: List[(String, String)]): Seq[String] = {

    val nodes = nodeList(document, expression, namespaces)

    (for {
      i <- 0 until nodes.getLength
    } yield {
      val item = nodes.item(i)
      item.getNodeType match {
        case Node.ELEMENT_NODE if item.getChildNodes.getLength > 0 =>
          val firstChild = item.getChildNodes.item(0)
          if (firstChild.getNodeType == Node.TEXT_NODE)
            Some(firstChild.getNodeValue)
          else None

        case _ =>
          Option(item.getNodeValue)
      }
    }).flatten
  }
}
