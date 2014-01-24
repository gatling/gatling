/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.filter

import scala.annotation.tailrec
import scala.util.matching.Regex
import com.typesafe.scalalogging.slf4j.StrictLogging
import scala.util.Try
import scala.util.Success

case class Filters(first: Filter, second: Filter) {

	def accept(url: String) = first.accept(url) && second.accept(url)
}

object SafeRegexes extends StrictLogging {
	def apply(patterns: List[String]): List[Regex] = {
		val (regexes, incorrectPatterns) = patterns.map(p => Try(p.r)).partition(_.isSuccess)

		incorrectPatterns.map(_.failed.get).foreach { exp =>
			logger.error("Incorrect filter pattern. " + exp.getMessage)
		}

		regexes.map(_.get)
	}
}

sealed abstract class Filter {
	def patterns: List[String]
	val regexes = SafeRegexes(patterns)
	def accept(url: String): Boolean
}

case class WhiteList(patterns: List[String] = Nil) extends Filter {
	def accept(url: String): Boolean = regexes.forall(_.pattern.matcher(url).matches)
}

case class BlackList(patterns: List[String] = Nil) extends Filter {
	def accept(url: String): Boolean = regexes.forall(!_.pattern.matcher(url).matches)
}
