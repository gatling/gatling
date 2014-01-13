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

import java.util.regex.Pattern

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedCountOption, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ noneSuccess, SuccessWrapper, Validation }
import jsr166e.ConcurrentHashMapV8

object RegexExtractor {

	val cache: concurrent.Map[String, Pattern] = new ConcurrentHashMapV8[String, Pattern]

	def cached(pattern: String) = if (configuration.core.extract.regex.cache) cache.getOrElseUpdate(pattern, Pattern.compile(pattern)) else Pattern.compile(pattern)

	def extractAll[X: GroupExtractor](chars: CharSequence, pattern: String): Seq[X] = {

		val matcher = cached(pattern).matcher(chars)
		matcher.foldLeft(List.empty[X]) { (matcher, values) =>
			matcher.value :: values
		}.reverse
	}
}

abstract class RegexExtractor[X] extends CriterionExtractor[CharSequence, String, X] { val criterionName = "regex" }

class SingleRegexExtractor[X: GroupExtractor](val criterion: String, occurrence: Int) extends RegexExtractor[X] {

	def extract(prepared: CharSequence): Validation[Option[X]] = {
		val matcher = RegexExtractor.cached(criterion).matcher(prepared)
		matcher.findMatchN(occurrence).success
	}
}

class MultipleRegexExtractor[X: GroupExtractor](val criterion: String) extends RegexExtractor[Seq[X]] {

	def extract(prepared: CharSequence): Validation[Option[Seq[X]]] = RegexExtractor.extractAll(prepared, criterion).liftSeqOption.success
}

class CountRegexExtractor(val criterion: String) extends RegexExtractor[Int] {

	def extract(prepared: CharSequence): Validation[Option[Int]] = {
		val matcher = RegexExtractor.cached(criterion).matcher(prepared)

		var count = 0
		while (matcher.find)
			count = count + 1

		count.liftCountOptionV
	}
}
