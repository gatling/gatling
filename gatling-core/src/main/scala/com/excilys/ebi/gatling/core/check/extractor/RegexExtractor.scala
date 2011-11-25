/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.check.extractor

import java.util.regex.Pattern

/**
 * This class is a built-in extractor that helps searching with Regular Expressions
 *
 * @constructor creates a new RegExpExtractor
 * @param textContent the text where the search will be made
 * @param occurence the occurrence of the result that should be returned
 */
class RegexExtractor(textContent: String, occurrence: Int) extends Extractor {
	/**
	 * The actual extraction happens here. The regular expression is compiled and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the regular expression to be matched
	 * @return an option containing the value if found, None otherwise
	 */
	def extract(expression: String): List[String] = {
		logger.debug("Extracting with expression : {}", expression)

		val matcher = Pattern.compile(expression).matcher(textContent)

		for (i <- 0 to occurrence) {
			if (!matcher.find)
				return Nil
		}

		// if a group is specified, return the group 1, else return group 0 (ie the match)
		List(new String(matcher.group(matcher.groupCount min 1)))
	}
}