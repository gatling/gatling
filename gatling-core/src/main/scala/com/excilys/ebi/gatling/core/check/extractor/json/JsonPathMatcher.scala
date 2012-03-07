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
package com.excilys.ebi.gatling.core.check.extractor.json
import scala.annotation.tailrec
import com.excilys.ebi.gatling.core.check.extractor.json.JsonTokenizer.unstack

object JsonPathMatcher {

	def matchPath(expected: List[JsonPathElement], actual: List[JsonPathElement]): Boolean = {

		@tailrec
		def pathMatchRec(expectedVsActualList: List[(JsonPathElement, JsonPathElement)]): Boolean = {
			expectedVsActualList match {
				case Nil => true
				case (expected, actual) :: others =>
					if (!expected.accept(actual))
						false
					else
						pathMatchRec(others)
			}
		}

		expected.length == actual.length && pathMatchRec(expected zip actual)
	}

	def matchPath(expected: String, actual: String): Boolean = {
		val actualPath = JsonTokenizer.tokenize(actual)
		val expectedPath = JsonTokenizer.tokenize(expected)
		val unstackedExpectedPath = unstack(expectedPath, actualPath.length)
		matchPath(unstackedExpectedPath, actualPath)
	}
}