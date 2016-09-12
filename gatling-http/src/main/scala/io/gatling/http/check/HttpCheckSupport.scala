/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.check

import io.gatling.commons.validation.Validation
import io.gatling.core.check.{ ConditionalCheck, TypedConditionalCheckWrapper, UntypedConditionalCheckWrapper }
import io.gatling.core.check.extractor.css.CssExtractorFactory
import io.gatling.core.check.extractor.jsonpath.JsonPathExtractorFactory
import io.gatling.core.check.extractor.regex.{ Patterns, RegexExtractorFactory }
import io.gatling.core.check.extractor.xpath.{ JdkXPathExtractorFactory, SaxonXPathExtractorFactory }
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.check.body._
import io.gatling.http.check.checksum.HttpChecksumCheckBuilder
import io.gatling.http.check.header.{ HttpHeaderCheckBuilder, HttpHeaderRegexCheckBuilder, HttpHeaderRegexExtractorFactory }
import io.gatling.http.check.status.HttpStatusCheckBuilder
import io.gatling.http.check.time.HttpResponseTimeCheckBuilder
import io.gatling.http.check.url.{ CurrentLocationCheckBuilder, CurrentLocationRegexCheckBuilder }
import io.gatling.http.response.Response

trait HttpCheckSupport {

  def regex(expression: Expression[String])(implicit extractorFactory: RegexExtractorFactory) =
    HttpBodyRegexCheckBuilder.regex(expression)

  val substring = HttpBodySubstringCheckBuilder.substring _

  def xpath(expression: Expression[String], namespaces: List[(String, String)] = Nil)(implicit extractorFactory: SaxonXPathExtractorFactory, jdkXPathExtractorFactory: JdkXPathExtractorFactory) =
    HttpBodyXPathCheckBuilder.xpath(expression, namespaces)

  def css(selector: Expression[String])(implicit extractorFactory: CssExtractorFactory) =
    HttpBodyCssCheckBuilder.css(selector, None)
  def css(selector: Expression[String], nodeAttribute: String)(implicit extractorFactory: CssExtractorFactory) =
    HttpBodyCssCheckBuilder.css(selector, Some(nodeAttribute))

  def form(selector: Expression[String])(implicit extractorFactory: CssExtractorFactory) = css(selector).ofType[Map[String, Seq[String]]]

  def jsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    HttpBodyJsonPathCheckBuilder.jsonPath(path)
  def jsonpJsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    HttpBodyJsonpJsonPathCheckBuilder.jsonpJsonPath(path)

  val bodyString = HttpBodyStringCheckBuilder.BodyString
  val bodyBytes = HttpBodyBytesCheckBuilder.BodyBytes

  val header = HttpHeaderCheckBuilder.header _

  implicit def defaultHttpHeaderRegexExtractorFactory(implicit patterns: Patterns) = new HttpHeaderRegexExtractorFactory

  def headerRegex(headerName: Expression[String], pattern: Expression[String])(implicit extractorFactory: HttpHeaderRegexExtractorFactory) =
    HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern)

  val status = HttpStatusCheckBuilder.Status

  val currentLocation = CurrentLocationCheckBuilder.CurrentLocation
  def currentLocationRegex(expression: Expression[String])(implicit extractorFactory: RegexExtractorFactory) =
    CurrentLocationRegexCheckBuilder.currentLocationRegex(expression)

  val md5 = HttpChecksumCheckBuilder.Md5
  val sha1 = HttpChecksumCheckBuilder.Sha1

  val responseTimeInMillis = HttpResponseTimeCheckBuilder.ResponseTimeInMillis

  implicit object HttpTypedConditionalCheckWrapper extends TypedConditionalCheckWrapper[Response, HttpCheck] {
    override def wrap(condition: (Response, Session) => Validation[Boolean], thenCheck: HttpCheck) =
      HttpCheck(ConditionalCheck(condition, thenCheck), thenCheck.scope, thenCheck.responseBodyUsageStrategy)
  }

  implicit object HttpUntypedConditionalCheckWrapper extends UntypedConditionalCheckWrapper[HttpCheck] {
    override def wrap(condition: Expression[Boolean], thenCheck: HttpCheck) = {
      val typedCondition = (response: Response, session: Session) => condition(session)
      HttpCheck(ConditionalCheck(typedCondition, thenCheck), thenCheck.scope, thenCheck.responseBodyUsageStrategy)
    }
  }

}
