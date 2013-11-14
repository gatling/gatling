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
package io.gatling.core.check.extractor.css

import java.util.{ List => JList }

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable

import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.{ Node, NodeSelector }
import jodd.util.StringUtil

object ExtendedNodeSelector {

	def parseQuery(query: String): Seq[JList[CssSelector]] = {
		val singleQueries = StringUtil.splitc(query, ',')
		singleQueries.map(new CSSelly(_).parse)
	}
}

class ExtendedNodeSelector(rootNode: Node) extends NodeSelector(rootNode) {

	def select(selectorsSeq: Seq[JList[CssSelector]]): Seq[Node] = {

		val results = mutable.ArrayBuffer.empty[Node]

		for {
			selectors <- selectorsSeq
			selectedNode <- select(rootNode, selectors) if !results.contains(selectedNode)
		} {
			results += selectedNode
		}

		results
	}
}
