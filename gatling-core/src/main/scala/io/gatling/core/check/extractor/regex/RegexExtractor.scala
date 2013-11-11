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

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedOption, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object RegexExtractor {

	val cache: concurrent.Map[String, Pattern] = new ConcurrentHashMap[String, Pattern]

	def cached(pattern: String) = if (configuration.core.extract.regex.cache) cache.getOrElseUpdate(pattern, Pattern.compile(pattern)) else Pattern.compile(pattern)

	def extractAll[X: GroupExtractor](string: String, pattern: String): Seq[X] = {

		val matcher = cached(pattern).matcher(string)
		matcher.foldLeft(List.empty[X]) { (matcher, values) =>
			matcher.value :: values
		}.reverse
	}
}

abstract class RegexExtractor[X] extends CriterionExtractor[String, String, X] { val name = "regex" }

class SingleRegexExtractor[X: GroupExtractor](val criterion: Expression[String], occurrence: Int) extends RegexExtractor[X] {

	def extract(prepared: String, criterion: String): Validation[Option[X]] = {
		val matcher = RegexExtractor.cached(criterion).matcher(prepared)
		matcher.findMatchN(occurrence).success
	}
}

class MultipleRegexExtractor[X: GroupExtractor](val criterion: Expression[String]) extends RegexExtractor[Seq[X]] {

	def extract(prepared: String, criterion: String): Validation[Option[Seq[X]]] = RegexExtractor.extractAll(prepared, criterion).liftSeqOption.success
}

class CountRegexExtractor(val criterion: Expression[String]) extends RegexExtractor[Int] {

	def extract(prepared: String, criterion: String): Validation[Option[Int]] = {
		val matcher = RegexExtractor.cached(criterion).matcher(prepared)
		matcher.foldLeft(0) { (_, count) =>
			count + 1
		}.liftOption.success
	}
}
