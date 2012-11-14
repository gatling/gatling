/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.{ asJavaIterator, asScalaIterator }

import org.jaxen.{ DefaultNavigator, JaxenConstants, NamedAccessNavigator }

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ ArrayNode, TextNode }

/**
 * A Jaxen navigator for a Jackson tree
 */
class JacksonNavigator extends DefaultNavigator with NamedAccessNavigator {

	def getAttributeAxisIterator(contextNode: Object, localName: String, namespacePrefix: String, namespaceURI: String) = getChildAxisIterator(contextNode, localName, namespacePrefix, namespaceURI)

	def getChildAxisIterator(contextNode: Object, localName: String, namespacePrefix: String, namespaceURI: String): java.util.Iterator[_] = {

		def getChildrenByName(node: JsonNode) =
			Option(node.get(localName)).map {
				_ match {
					case array: ArrayNode => array.elements
					case node => java.util.Collections.singleton[JsonNode](node).iterator: java.util.Iterator[_]
				}
			}.getOrElse(JaxenConstants.EMPTY_ITERATOR)

		contextNode match {
			case array: ArrayNode => array.elements.flatMap(getChildrenByName)
			case node: JsonNode => getChildrenByName(node)
		}
	}

	override def getAttributeAxisIterator(contextNode: Object) = getChildAxisIterator(contextNode)

	override def getChildAxisIterator(contextNode: Object) = contextNode.asInstanceOf[JsonNode].iterator

	override def getDocumentNode(contextNode: Object) = contextNode

	def getTextStringValue(text: Object) = getElementStringValue(text)

	def isAttribute(contextNode: Object) = if (contextNode.isInstanceOf[JsonNode]) contextNode.asInstanceOf[JsonNode].isValueNode else false

	def isElement(contextNode: Object) = !isAttribute(contextNode)

	def isText(contextNode: Object) = contextNode.isInstanceOf[TextNode]

	def getElementStringValue(element: Object) = element match {
		case e: JsonNode => e.asText
		case _ => element.toString
	}

	def getElementName(contextNode: Object) = EMPTY

	def getElementNamespaceUri(contextNode: Object) = EMPTY

	// unupported operations
	def getAttributeName(attr: Object) = throw new UnsupportedOperationException

	def getAttributeNamespaceUri(attr: Object) = throw new UnsupportedOperationException

	def getAttributeQName(attr: Object) = throw new UnsupportedOperationException

	def getAttributeStringValue(attr: Object) = throw new UnsupportedOperationException

	def getCommentStringValue(attr: Object) = throw new UnsupportedOperationException

	def getElementQName(contextNode: Object) = throw new UnsupportedOperationException

	def getNamespacePrefix(namespace: Object) = throw new UnsupportedOperationException

	def getNamespaceStringValue(namespace: Object) = throw new UnsupportedOperationException

	def parseXPath(xpath: String) = throw new UnsupportedOperationException

	def isComment(obj: Object) = false

	def isNamespace(obj: Object) = false

	def isProcessingInstruction(obj: Object) = false

	def isDocument(obj: Object) = false
}