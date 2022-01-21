/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.io.InputStream

import io.gatling.commons.validation.Validation
import io.gatling.core.check.bytes._
import io.gatling.core.check.checksum._
import io.gatling.core.check.css._
import io.gatling.core.check.jmespath._
import io.gatling.core.check.jsonpath._
import io.gatling.core.check.regex._
import io.gatling.core.check.stream._
import io.gatling.core.check.string._
import io.gatling.core.check.substring._
import io.gatling.core.check.time._
import io.gatling.core.check.xpath._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.message.ResponseTimings

import com.fasterxml.jackson.databind.JsonNode
import io.burt.jmespath.function.{ Function => JmesPathFunction }
import jodd.lagarto.dom.NodeSelector
import net.sf.saxon.s9api.XdmNode

trait CheckSupport {

  implicit def validate2Final[T, P, X](validatorCheckBuilder: CheckBuilder.Validate[T, P, X]): CheckBuilder.Final[T, P] =
    validatorCheckBuilder.exists
  implicit def find2Validate[T, P, X](findCheckBuilder: CheckBuilder.Find[T, P, X]): CheckBuilder.Validate[T, P, X] =
    findCheckBuilder.find
  implicit def find2Final[T, P, X](findCheckBuilder: CheckBuilder.Find[T, P, X]): CheckBuilder.Final[T, P] =
    findCheckBuilder.find.exists

  def checkIf[C <: Check[_]](condition: Expression[Boolean])(thenCheck: C)(implicit maker: UntypedCheckIfMaker[C]): C =
    maker.make(thenCheck, condition)

  def checkIf[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: C)(implicit maker: TypedCheckIfMaker[R, C]): C =
    maker.make(thenCheck, condition)

  def regex(pattern: Expression[String])(implicit patterns: Patterns): CheckBuilder.MultipleFind[RegexCheckType, String, String] with RegexOfType =
    RegexCheckBuilder.regex(pattern, patterns)

  val bodyString: CheckBuilder.Find[BodyStringCheckType, String, String] = BodyStringCheckBuilder

  val bodyBytes: CheckBuilder.Find[BodyBytesCheckType, Array[Byte], Array[Byte]] = BodyBytesCheckBuilder

  val bodyLength: CheckBuilder.Find[BodyBytesCheckType, Int, Int] = BodyLengthCheckBuilder

  val bodyStream: CheckBuilder.Find[BodyStreamCheckType, () => InputStream, InputStream] = BodyStreamCheckBuilder

  def substring(pattern: Expression[String]): CheckBuilder.MultipleFind[SubstringCheckType, String, Int] = new SubstringCheckBuilder(pattern)

  def xpath(path: Expression[String])(implicit xmlParsers: XmlParsers): CheckBuilder.MultipleFind[XPathCheckType, XdmNode, String] =
    xpath(path, Map.empty[String, String])
  def xpath(path: Expression[String], namespaces: Map[String, String])(implicit
      xmlParsers: XmlParsers
  ): CheckBuilder.MultipleFind[XPathCheckType, XdmNode, String] =
    new XPathCheckBuilder(path, namespaces, xmlParsers)

  def css(selector: Expression[String])(implicit selectors: CssSelectors): CheckBuilder.MultipleFind[CssCheckType, NodeSelector, String] with CssOfType =
    CssCheckBuilder.css(selector, None, selectors)
  def css(selector: Expression[String], nodeAttribute: String)(implicit
      selectors: CssSelectors
  ): CheckBuilder.MultipleFind[CssCheckType, NodeSelector, String] with CssOfType =
    CssCheckBuilder.css(selector, Some(nodeAttribute), selectors)
  def form(selector: Expression[String])(implicit selectors: CssSelectors): CheckBuilder.MultipleFind[CssCheckType, NodeSelector, Map[String, Any]] =
    css(selector).ofType[Map[String, Any]]

  def jsonPath(path: Expression[String])(implicit jsonPaths: JsonPaths): CheckBuilder.MultipleFind[JsonPathCheckType, JsonNode, String] with JsonPathOfType =
    JsonPathCheckBuilder.jsonPath(path, jsonPaths)

  def jmesPath(path: Expression[String])(implicit jmesPaths: JmesPaths): CheckBuilder.Find[JmesPathCheckType, JsonNode, String] with JmesPathOfType =
    JmesPathCheckBuilder.jmesPath(path, jmesPaths)

  def jsonpJsonPath(
      path: Expression[String]
  )(implicit jsonPaths: JsonPaths): CheckBuilder.MultipleFind[JsonpJsonPathCheckType, JsonNode, String] with JsonpJsonPathOfType =
    JsonpJsonPathCheckBuilder.jsonpJsonPath(path, jsonPaths)

  def jsonpJmesPath(
      path: Expression[String]
  )(implicit jmesPaths: JmesPaths): CheckBuilder.Find[JsonpJmesPathCheckType, JsonNode, String] with JsonpJmesPathOfType =
    JsonpJmesPathCheckBuilder.jsonpJmesPath(path, jmesPaths)

  def registerJmesPathFunctions(functions: JmesPathFunction*): Unit = {
    require(!functions.contains(null), "JMESPath functions can't contain null elements")
    JmesPathFunctions.register(functions)
  }

  val md5: CheckBuilder.Find[Md5CheckType, String, String] = ChecksumCheckBuilder.Md5
  val sha1: CheckBuilder.Find[Sha1CheckType, String, String] = ChecksumCheckBuilder.Sha1

  val responseTimeInMillis: CheckBuilder.Find[ResponseTimeCheckType, ResponseTimings, Int] = ResponseTimeCheckBuilder.ResponseTimeInMillis
}
