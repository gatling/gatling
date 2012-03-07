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
import java.util.regex.Pattern

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

class JsonPathTokenizerException(expression: String) extends IllegalArgumentException("Malformed expression: " + expression)

object JsonPathTokenizer {

	val SPLIT_PATTERN = Pattern.compile("/")
	val ARRAY_ELEMENT_PATTERN = Pattern.compile("""(.+?)\[(\d+?)\]""")

	def tokenize(string: String): List[JsonPathElement] = {

		@tailrec
		def analyze(strings: List[String], elements: List[JsonPathElement]): List[JsonPathElement] = strings match {
			case Nil => elements
			case string :: others => {
				val element = string match {
					case EMPTY =>
						if (elements == Nil)
							JsonRootWildCard
						else
							throw new JsonPathTokenizerException(string)
					case "*" => NodeWildCard
					case str =>
						val matcher = ARRAY_ELEMENT_PATTERN.matcher(str)
						if (matcher.find)
							ArrayElementNode(matcher.group(1), matcher.group(2).toInt)
						else
							SimpleNode(str)
				}
				analyze(others, element :: elements)
			}
		}

		if (!string.startsWith("/"))
			throw new JsonPathTokenizerException(string)

		analyze(SPLIT_PATTERN.split(string.stripPrefix("/"), 0).toList, Nil)
	}

	def unstack(expected: List[JsonPathElement], actualLength: Int): List[JsonPathElement] = expected.last match {
		case JsonRootWildCard => expected.init.padTo(actualLength, NodeWildCard)
		case _ => expected
	}
}