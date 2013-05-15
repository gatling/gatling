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
package com.excilys.ebi.gatling.core.check.extractor.regex

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.matching.Regex

import com.excilys.ebi.gatling.core.check.extractor.Extractor
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.util.StringHelper.substringCopiesCharArray

object RegexExtractor {

	val cache = mutable.Map.empty[String, Regex]
	def cachedRegex(pattern: String) = if (configuration.core.extract.regex.cache) cache.getOrElseUpdate(pattern, pattern.r) else pattern.r
}

/**
 * A built-in extractor for extracting values with Regular Expressions
 *
 * @constructor creates a new RegExpExtractor
 * @param textContent the text where the search will be made
 */
class RegexExtractor(textContent: String) extends Extractor {

	import RegexExtractor._

	/**
	 * The actual extraction happens here. The regular expression is compiled and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the regular expression to be matched
	 * @return an option containing the value if found, None otherwise
	 */
	def extractOne(occurrence: Int)(expression: String): Option[String] = {

		val matcher = cachedRegex(expression).pattern.matcher(textContent)

		@tailrec
		def findRec(countDown: Int): Boolean = {
			if (!matcher.find)
				false
			else if (countDown == 0)
				true
			else
				findRec(countDown - 1)
		}

		if (findRec(occurrence)) {
			// if a group is specified, return the group 1, else return group 0 (ie the match)
			val value = matcher.group(matcher.groupCount.min(1))
			if (substringCopiesCharArray) value
			else new String(value)
		} else
			None
	}

	/**
	 * The actual extraction happens here. The regular expression is compiled and the occurrence-th
	 * result is returned if existing.
	 *
	 * @param expression a String containing the regular expression to be matched
	 * @return an option containing the value if found, None otherwise
	 */
	def extractMultiple(expression: String): Option[Seq[String]] = cachedRegex(expression).findAllIn(textContent).matchData.map { matcher =>
		val value = matcher.group(1 min matcher.groupCount)
		if (substringCopiesCharArray) value
		else new String(value)
	}.toList // very important: Iterator.toSeq produces a Stream, so map function is only evaluated lazily and the original byte array can't be GCed.

	def count(expression: String): Option[Int] = cachedRegex(expression).findAllIn(textContent).size
}