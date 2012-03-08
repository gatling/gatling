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

sealed trait JsonPathElement {
	def accept(other: JsonPathElement): Boolean
}

case object JsonRootWildCard extends JsonPathElement {
	def accept(other: JsonPathElement) = throw new UnsupportedOperationException("JsonRootWildCard shouldn't be used for path matching")
}

case object NodeWildCard extends JsonPathElement {
	def accept(other: JsonPathElement) = true
}

case class SimpleNode(name: String) extends JsonPathElement {
	def accept(other: JsonPathElement) = other match {
		case SimpleNode(otherName) if (name == otherName) => true
		case ArrayElementNode(otherName, _) if (name == otherName) => true
		case _ => false
	}
}

case class ArrayElementNode(name: String, index: Int) extends JsonPathElement {
	def accept(other: JsonPathElement) = other match {
		case ArrayElementNode(otherName, otherIndex) if (name == otherName && index == otherIndex) => true
		case _ => false
	}
}