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
package io.gatling.core.check.extractor.regex

import java.util.regex.Pattern

import io.gatling.core.util.CacheHelper

import io.gatling.core.check.extractor._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object RegexExtractor {

  lazy val Cache = CacheHelper.newCache[String, Pattern](configuration.core.extract.regex.cacheMaxCapacity)

  def cached(pattern: String) =
    if (configuration.core.extract.regex.cacheMaxCapacity > 0) Cache.getOrElseUpdate(pattern, Pattern.compile(pattern))
    else Pattern.compile(pattern)

  def extractAll[X: GroupExtractor](chars: CharSequence, pattern: String): Seq[X] = {

    val matcher = cached(pattern).matcher(chars)
    matcher.foldLeft(List.empty[X]) { (matcher, values) =>
      matcher.value :: values
    }.reverse
  }
}

abstract class RegexExtractor[X] extends CriterionExtractor[CharSequence, String, X] { val criterionName = "regex" }

class SingleRegexExtractor[X: GroupExtractor](val criterion: String, val occurrence: Int) extends RegexExtractor[X] with FindArity {

  def extract(prepared: CharSequence): Validation[Option[X]] = {
    val matcher = RegexExtractor.cached(criterion).matcher(prepared)
    matcher.findMatchN(occurrence).success
  }
}

class MultipleRegexExtractor[X: GroupExtractor](val criterion: String) extends RegexExtractor[Seq[X]] with FindAllArity {

  def extract(prepared: CharSequence): Validation[Option[Seq[X]]] = RegexExtractor.extractAll(prepared, criterion).liftSeqOption.success
}

class CountRegexExtractor(val criterion: String) extends RegexExtractor[Int] with CountArity {

  def extract(prepared: CharSequence): Validation[Option[Int]] = {
    val matcher = RegexExtractor.cached(criterion).matcher(prepared)

    var count = 0
    while (matcher.find)
      count = count + 1

    Some(count).success
  }
}
