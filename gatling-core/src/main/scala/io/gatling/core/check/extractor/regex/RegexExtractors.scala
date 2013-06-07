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
package io.gatling.core.check.extractor.regex

import java.util.regex.{ Matcher, Pattern }

import scala.annotation.tailrec
import scala.collection.mutable

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.{ LiftedOption, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.StringHelper.substringCopiesCharArray
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object RegexExtractors {

	val cache = mutable.Map.empty[String, Pattern]
	def cachedRegex(pattern: String) = if (configuration.core.extract.regex.cache) cache.getOrElseUpdate(pattern, Pattern.compile(pattern)) else Pattern.compile(pattern)

	abstract class RegexExtractor[X] extends Extractor[String, String, X] {
		val name = "regex"
	}

	implicit class RichMatcher(val matcher: Matcher) extends AnyVal {

		def foldLeft[T](zero: T)(f: (Matcher, T) => T): T = {
			var temp = zero
			while (matcher.find)
				temp = f(matcher, temp)
			temp
		}

		def findMatchN(n: Int): Option[String] = {

			@tailrec
			def findRec(countDown: Int): Boolean = {
				if (!matcher.find)
					false
				else if (countDown == 0)
					true
				else
					findRec(countDown - 1)
			}

			if (findRec(n))
				value.liftOption
			else
				None
		}

		def value = {
			// if a group is specified, return the group 1, else return group 0 (ie the match)
			val value = matcher.group(matcher.groupCount min 1)
			if (substringCopiesCharArray) value
			else new String(value)
		}
	}

	def extract(string: String, pattern: String): Seq[String] = {

		val matcher = cachedRegex(pattern).matcher(string)
		matcher.foldLeft(List.empty[String]) { (matcher, values) =>
			matcher.value :: values
		}.reverse
	}

	def extractOne(occurrence: Int) = new RegexExtractor[String] {

		def apply(prepared: String, criterion: String): Validation[Option[String]] = {
			val matcher = cachedRegex(criterion).matcher(prepared)
			matcher.findMatchN(occurrence).success
		}
	}

	val extractMultiple = new RegexExtractor[Seq[String]] {

		def apply(prepared: String, criterion: String): Validation[Option[Seq[String]]] = extract(prepared, criterion).liftSeqOption.success
	}

	val count = new RegexExtractor[Int] {

		def apply(prepared: String, criterion: String): Validation[Option[Int]] = {
			val matcher = cachedRegex(criterion).matcher(prepared)
			matcher.foldLeft(0) { (_, count) =>
				count + 1
			}.liftOption.success
		}
	}
}