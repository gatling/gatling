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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.asJavaIterator

import org.jaxen.{ DefaultNavigator, JaxenConstants }

/**
 * A Jaxen navigator for a Jackson tree
 */
object JacksonNavigator extends DefaultNavigator {

	override def getChildAxisIterator(contextNode: Object): java.util.Iterator[_] = contextNode match {
		case element: JsonElement => element.children.iterator
		case text: JsonText => JaxenConstants.EMPTY_ITERATOR
	}

	override def getDocumentNode(contextNode: Object) = contextNode

	def getTextStringValue(text: Object) = getElementStringValue(text)

	def isAttribute(contextNode: Object) = false

	def isElement(contextNode: Object) = true

	def isText(contextNode: Object) = false

	def getElementStringValue(element: Object) = element match {
		case text: JsonText => text.value
		case _ => element.toString
	}

	def getElementName(contextNode: Object) = contextNode.asInstanceOf[JsonNode].name

	def getElementQName(contextNode: Object) = getElementName(contextNode)

	def getElementNamespaceUri(contextNode: Object) = null

	def isComment(obj: Object) = false

	def isNamespace(obj: Object) = false

	def isProcessingInstruction(obj: Object) = false

	def isDocument(obj: Object) = false

	// unsupported operations
	def getAttributeName(attr: Object) = throw new UnsupportedOperationException

	def getAttributeNamespaceUri(attr: Object) = throw new UnsupportedOperationException

	def getAttributeQName(attr: Object) = throw new UnsupportedOperationException

	def getAttributeStringValue(attr: Object) = throw new UnsupportedOperationException

	def getCommentStringValue(attr: Object) = throw new UnsupportedOperationException

	def getNamespacePrefix(namespace: Object) = throw new UnsupportedOperationException

	def getNamespaceStringValue(namespace: Object) = throw new UnsupportedOperationException

	def parseXPath(xpath: String) = throw new UnsupportedOperationException
}