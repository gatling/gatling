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

import java.io.StringReader
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers._
import javax.xml.xpath.XPathConstants.NODESET
import javax.xml.xpath._

import org.w3c.dom.{ Node, NodeList, Document }

import org.xml.sax.{ EntityResolver, InputSource }

import io.gatling.core.check.extractor._
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object JdkXPathExtractor {

  val XPathFactoryTL = new ThreadLocal[XPathFactory] {
    override def initialValue() = XPathFactory.newInstance
  }

  lazy val DocumentBuilderFactoryInstance = {

    System.setProperty("org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault")
    System.setProperty("com.sun.org.apache.xml.internal.dtm.DTMManager", "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault")
    System.setProperty("javax.xml.xpath.XPathFactory", "org.apache.xpath.jaxp.XPathFactoryImpl")

    val instance = DocumentBuilderFactory.newInstance
    instance.setExpandEntityReferences(false)
    instance.setNamespaceAware(true)
    instance
  }

  val NoopEntityResolver = new EntityResolver {
    // FIXME can't we create only one StringReader?
    def resolveEntity(publicId: String, systemId: String) = new InputSource(new StringReader(""))
  }

  val DocumentBuilderTL = new ThreadLocal[DocumentBuilder] {
    override def initialValue() = {
      try {
        val builder = DocumentBuilderFactoryInstance.newDocumentBuilder
        builder.setEntityResolver(NoopEntityResolver)
        builder
      } catch {
        case e: ParserConfigurationException => throw new RuntimeException(e)
      }
    }
  }

  def parse(inputSource: InputSource): Document =
    DocumentBuilderTL.get.parse(inputSource)

  abstract class JDKXPathExtractor[X] extends CriterionExtractor[Option[Document], String, X] {

    val criterionName = "xpath"

    def nodeList(document: Document, expression: String, namespaces: List[(String, String)]): NodeList = {
      val path = XPathFactoryTL.get.newXPath

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
        item = nodes.item(i)
      } yield {
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

  class SingleXPathExtractor(val criterion: String, namespaces: List[(String, String)], val occurrence: Int) extends JDKXPathExtractor[String] with FindArity {

    def extract(prepared: Option[Document]): Validation[Option[String]] =
      prepared.flatMap(document => extractAll(document, criterion, namespaces).lift(occurrence)).success
  }

  class MultipleXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends JDKXPathExtractor[Seq[String]] with FindAllArity {

    def extract(prepared: Option[Document]): Validation[Option[Seq[String]]] =
      prepared.flatMap(document => extractAll(document, criterion, namespaces).liftSeqOption).success
  }

  class CountXPathExtractor(val criterion: String, namespaces: List[(String, String)]) extends JDKXPathExtractor[Int] with CountArity {

    def extract(prepared: Option[Document]): Validation[Option[Int]] =
      prepared.map(document => nodeList(document, criterion, namespaces).getLength).success
  }
}
