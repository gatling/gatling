/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check

import io.gatling.core.session.{ Expression, Session }
import io.gatling.commons.validation.Validation
import io.gatling.core.check.extractor.bytes._
import io.gatling.core.check.extractor.checksum._
import io.gatling.core.check.extractor.css._
import io.gatling.core.check.extractor.jmespath._
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.check.extractor.regex._
import io.gatling.core.check.extractor.string._
import io.gatling.core.check.extractor.substring._
import io.gatling.core.check.extractor.time._
import io.gatling.core.check.extractor.xpath._
import io.gatling.core.stats.message.ResponseTimings

import com.fasterxml.jackson.databind.JsonNode
import io.burt.jmespath.function.{ Function => JmesPathFunction }
import jodd.lagarto.dom.NodeSelector

trait CheckSupport {

  implicit def validatorCheckBuilder2CheckBuilder[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X]): CheckBuilder[A, P, X] with SaveAs[A, P, X] = validatorCheckBuilder.exists
  implicit def findCheckBuilder2ValidatorCheckBuilder[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X]): ValidatorCheckBuilder[A, P, X] = findCheckBuilder.find
  implicit def findCheckBuilder2CheckBuilder[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X]): CheckBuilder[A, P, X] with SaveAs[A, P, X] = findCheckBuilder.find.exists

  def checkIf[C <: Check[_]](condition: Expression[Boolean])(thenCheck: C)(implicit cw: UntypedConditionalCheckWrapper[C]): C =
    cw.wrap(condition, thenCheck)

  def checkIf[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: C)(implicit cw: TypedConditionalCheckWrapper[R, C]): C =
    cw.wrap(condition, thenCheck)

  def regex(pattern: Expression[String])(implicit patterns: Patterns): MultipleFindCheckBuilder[RegexCheckType, CharSequence, String] with RegexOfType = RegexCheckBuilder.regex(pattern, patterns)

  val bodyString: FindCheckBuilder[BodyStringCheckType, String, String] = BodyStringCheckBuilder.BodyString

  val bodyBytes: FindCheckBuilder[BodyBytesCheckType, Array[Byte], Array[Byte]] = BodyBytesCheckBuilder.BodyBytes

  def substring(pattern: Expression[String]): MultipleFindCheckBuilder[SubstringCheckType, String, Int] = new SubstringCheckBuilder(pattern)

  def xpath(path: Expression[String], namespaces: List[(String, String)] = Nil)(implicit xmlParsers: XmlParsers): MultipleFindCheckBuilder[XPathCheckType, Option[Dom], String] =
    new XPathCheckBuilder(path, namespaces, xmlParsers)

  def css(selector: Expression[String])(implicit selectors: CssSelectors): MultipleFindCheckBuilder[CssCheckType, NodeSelector, String] with CssOfType =
    CssCheckBuilder.css(selector, None, selectors)
  def css(selector: Expression[String], nodeAttribute: String)(implicit selectors: CssSelectors): MultipleFindCheckBuilder[CssCheckType, NodeSelector, String] with CssOfType =
    CssCheckBuilder.css(selector, Some(nodeAttribute), selectors)
  def form(selector: Expression[String])(implicit selectors: CssSelectors): MultipleFindCheckBuilder[CssCheckType, NodeSelector, Map[String, Any]] = css(selector).ofType[Map[String, Any]]

  def jsonPath(path: Expression[String])(implicit jsonPaths: JsonPaths): MultipleFindCheckBuilder[JsonPathCheckType, JsonNode, String] with JsonPathOfType =
    JsonPathCheckBuilder.jsonPath(path, jsonPaths)

  def jmesPath(path: Expression[String])(implicit jmesPaths: JmesPaths): FindCheckBuilder[JmesPathCheckType, JsonNode, String] with JmesPathOfType =
    JmesPathCheckBuilder.jmesPath(path, jmesPaths)

  def jsonpJsonPath(path: Expression[String])(implicit jsonPaths: JsonPaths): MultipleFindCheckBuilder[JsonpJsonPathCheckType, JsonNode, String] with JsonpJsonPathOfType =
    JsonpJsonPathCheckBuilder.jsonpJsonPath(path, jsonPaths)

  def jsonpJmesPath(path: Expression[String])(implicit jmesPaths: JmesPaths): FindCheckBuilder[JsonpJmesPathCheckType, JsonNode, String] with JsonpJmesPathOfType =
    JsonpJmesPathCheckBuilder.jsonpJmesPath(path, jmesPaths)

  def registerJmesPathFunctions(functions: JmesPathFunction*): Unit = JmesPathFunctions.register(functions)

  val md5: FindCheckBuilder[Md5CheckType, String, String] = ChecksumCheckBuilder.Md5
  val sha1: FindCheckBuilder[Sha1CheckType, String, String] = ChecksumCheckBuilder.Sha1

  val responseTimeInMillis: FindCheckBuilder[ResponseTimeCheckType, ResponseTimings, Int] = ResponseTimeCheckBuilder.ResponseTimeInMillis
}
