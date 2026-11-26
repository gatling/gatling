/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.check.header

import java.{ lang => jl }

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.regex._
import io.gatling.core.session._
import io.gatling.http.check.{ HttpCheck, HttpCheckMaterializer }
import io.gatling.http.check.HttpCheckScope.Header
import io.gatling.http.response.Response

trait HttpRawHeadersRegexCheckType

trait HttpRawHeadersRegexOfType {
  self: HttpRawHeadersRegexCheckBuilder[String] =>

  def ofType[X: GroupExtractor]: CheckBuilder.MultipleFind[HttpRawHeadersRegexCheckType, String, X] =
    new HttpRawHeadersRegexCheckBuilder[X](pattern, patterns)
}

object HttpRawHeadersRegexCheckBuilder {
  def rawHeadersRegex(pattern: Expression[String], patterns: Patterns): HttpRawHeadersRegexCheckBuilder[String] with HttpRawHeadersRegexOfType =
    new HttpRawHeadersRegexCheckBuilder[String](pattern, patterns) with HttpRawHeadersRegexOfType
}

class HttpRawHeadersRegexCheckBuilder[X: GroupExtractor] private[header] (
    private[header] val pattern: Expression[String],
    private[header] val patterns: Patterns
) extends CheckBuilder.MultipleFind.Default[HttpRawHeadersRegexCheckType, String, X](logActualValueInError = false) {
  override protected def findExtractor(occurrence: Int): Expression[Extractor[String, X]] =
    pattern.map(RegexExtractors.find[X]("rawHeadersRegex", _, occurrence, patterns))
  override protected def findAllExtractor: Expression[Extractor[String, Seq[X]]] = pattern.map(RegexExtractors.findAll[X]("rawHeadersRegex", _, patterns))
  override protected def countExtractor: Expression[Extractor[String, Int]] = pattern.map(RegexExtractors.count("rawHeadersRegex", _, patterns))
}

object HttpRawHeadersRegexCheckMaterializer {

  /**
   * Status line followed by one "Name: value" line per header, each terminated by '\n'.
   */
  private[header] val RawHeadersPreparer: Preparer[Response, String] = response => {
    val sb = new jl.StringBuilder(256)
      .append(if (response.isHttp2) "HTTP/2" else "HTTP/1.1")
      .append(' ')
      .append(response.status.code)
      .append(' ')
      .append(response.status.reasonPhrase)
      .append('\n')
    response.headers.iteratorCharSequence().forEachRemaining { entry =>
      sb.append(entry.getKey).append(": ").append(entry.getValue).append('\n'): Unit
    }
    sb.toString.success
  }

  val Instance: CheckMaterializer[HttpRawHeadersRegexCheckType, HttpCheck, Response, String] =
    new HttpCheckMaterializer[HttpRawHeadersRegexCheckType, String](Header, RawHeadersPreparer)
}
