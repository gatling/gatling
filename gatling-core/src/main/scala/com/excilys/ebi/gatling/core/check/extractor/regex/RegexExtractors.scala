/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.util.regex.Pattern

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.check.extractor.Extractors

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

object RegexExtractors extends Extractors {

	abstract class RegexExtractor[X] extends Extractor[String, String, X] {
		val name = "regex"
	}

	def extract(string: String, pattern: String): Seq[String] = pattern.r.findAllIn(string).matchData.map { matcher =>
		new String(matcher.group(1 min matcher.groupCount))
	}.toSeq

	def extractOne(occurrence: Int) = new RegexExtractor[String] {

		def apply(prepared: String, criterion: String): Validation[String, Option[String]] = {

			val matcher = Pattern.compile(prepared).matcher(prepared)

			@tailrec
			def findRec(countDown: Int): Boolean = {
				if (!matcher.find)
					false
				else if (countDown == 0)
					true
				else
					findRec(countDown - 1)
			}

			val value = if (findRec(occurrence))
				// if a group is specified, return the group 1, else return group 0 (ie the match)
				new String(matcher.group(matcher.groupCount.min(1))).liftOption
			else
				None

			value.success
		}
	}

	val extractMultiple = new RegexExtractor[Seq[String]] {

		def apply(prepared: String, criterion: String): Validation[String, Option[Seq[String]]] =

			criterion.r.findAllIn(prepared).matchData.map { matcher =>
				new String(matcher.group(1 min matcher.groupCount))
			}.toSeq.liftSeqOption.success
	}

	val count = new RegexExtractor[Int] {

		def apply(prepared: String, criterion: String): Validation[String, Option[Int]] =
			criterion.r.findAllIn(prepared).size.liftOption.success
	}
}