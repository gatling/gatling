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

import io.gatling.core.check.extractor._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.cache._
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object RegexExtractor {

  val PatternCache = ThreadSafeCache[String, Pattern](configuration.core.extract.regex.cacheMaxCapacity)
  val PatternCacheEnabled = configuration.core.extract.regex.cacheMaxCapacity > 0
}

trait RegexExtractor {

  import RegexExtractor._

  def compilePattern(regex: String) =
    if (PatternCacheEnabled)
      PatternCache.getOrElsePutIfAbsent(regex, Pattern.compile(regex))
    else
      Pattern.compile(regex)

  def extractAll[X: GroupExtractor](chars: CharSequence, pattern: String): Seq[X] = {

    val matcher = compilePattern(pattern).matcher(chars)
    matcher.foldLeft(List.empty[X]) { (matcher, values) =>
      matcher.value :: values
    }.reverse
  }
}

abstract class RegexExtractorBase[X] extends CriterionExtractor[CharSequence, String, X] with RegexExtractor {

  val criterionName = "regex"
}

class SingleRegexExtractor[X: GroupExtractor](val criterion: String, val occurrence: Int) extends RegexExtractorBase[X] with FindArity {

  def extract(prepared: CharSequence): Validation[Option[X]] = {
    val matcher = compilePattern(criterion).matcher(prepared)
    matcher.findMatchN(occurrence).success
  }
}

class MultipleRegexExtractor[X: GroupExtractor](val criterion: String) extends RegexExtractorBase[Seq[X]] with FindAllArity {

  def extract(prepared: CharSequence): Validation[Option[Seq[X]]] = extractAll(prepared, criterion).liftSeqOption.success
}

class CountRegexExtractor(val criterion: String) extends RegexExtractorBase[Int] with CountArity {

  def extract(prepared: CharSequence): Validation[Option[Int]] = {
    val matcher = compilePattern(criterion).matcher(prepared)

    var count = 0
    while (matcher.find)
      count = count + 1

    Some(count).success
  }
}
