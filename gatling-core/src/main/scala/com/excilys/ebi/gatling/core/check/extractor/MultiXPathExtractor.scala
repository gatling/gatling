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
package com.excilys.ebi.gatling.core.check.extractor

import scala.collection.JavaConversions.asScalaBuffer

import org.jaxen.dom.DOMXPath
import org.jaxen.XPath
import org.w3c.dom.{Node, Document}

class MultiXPathExtractor(document: Document) extends Extractor[List[String]] {

	/**
	 * The actual extraction happens here. The XPath expression is searched for and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the XPath expression to be searched
	 * @return an option containing the value if found, None otherwise
	 */
	def extract(expression: String) = {
		val xpathExpression: XPath = new DOMXPath(expression);
		xpathExpression.selectNodes(document).asInstanceOf[java.util.List[Node]].map(_.getTextContent).toList
	}
}