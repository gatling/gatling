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
package io.gatling.core.check

import io.gatling.core.session.Expression
import io.gatling.core.session.Session
import io.gatling.commons.validation.Validation
import io.gatling.core.check.extractor.bytes.BodyBytesCheckBuilder
import io.gatling.core.check.extractor.checksum.ChecksumCheckBuilder
import io.gatling.core.check.extractor.css.{CssCheckBuilder, CssSelectors}
import io.gatling.core.check.extractor.jsonpath.{JsonPathCheckBuilder, JsonPaths, JsonpJsonPathCheckBuilder}
import io.gatling.core.check.extractor.regex.{Patterns, RegexCheckBuilder, RegexOfType}
import io.gatling.core.check.extractor.string.BodyStringCheckBuilder
import io.gatling.core.check.extractor.substring.SubstringCheckBuilder
import io.gatling.core.check.extractor.xpath.{JdkXmlParsers, Saxon, XPathCheckBuilder}
import io.gatling.core.time.ResponseTimeCheckBuilder

trait CheckSupport {

  implicit def validatorCheckBuilder2CheckBuilder[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X]) = validatorCheckBuilder.exists
  implicit def findCheckBuilder2ValidatorCheckBuilder[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X]) = findCheckBuilder.find
  implicit def findCheckBuilder2CheckBuilder[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X]) = findCheckBuilder.find.exists

  @deprecated
  implicit def oldCheckBuilder2Check[C <: Check[R], R, P, X](checkBuilder: OldCheckBuilder[C, R, P, X]) = checkBuilder.build
  @deprecated
  implicit def oldValidatorCheckBuilder2CheckBuilder[C <: Check[R], R, P, X](validatorCheckBuilder: OldValidatorCheckBuilder[C, R, P, X]) = validatorCheckBuilder.exists
  @deprecated
  implicit def oldValidatorCheckBuilder2Check[C <: Check[R], R, P, X](validatorCheckBuilder: OldValidatorCheckBuilder[C, R, P, X]) = validatorCheckBuilder.exists.build
  @deprecated
  implicit def oldFindCheckBuilder2ValidatorCheckBuilder[C <: Check[R], R, P, X](findCheckBuilder: OldFindCheckBuilder[C, R, P, X]) = findCheckBuilder.find
  @deprecated
  implicit def oldFindCheckBuilder2CheckBuilder[C <: Check[R], R, P, X](findCheckBuilder: OldFindCheckBuilder[C, R, P, X]) = findCheckBuilder.find.exists
  @deprecated
  implicit def oldFindCheckBuilder2Check[C <: Check[R], R, P, X](findCheckBuilder: OldFindCheckBuilder[C, R, P, X]) = findCheckBuilder.find.exists.build

  implicit def conditionalCheckBuilder2Check[R, C <: Check[R]](conditionalCheckBuilder: ConditionalCheckBuilder[R, C]): ConditionalCheck[R, C] = conditionalCheckBuilder.build
  def checkIf[R, C <: Check[R]](condition: Expression[Boolean])(thenCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = cw.wrap(ConditionalCheckBuilder((r: R, s: Session) => condition(s), thenCheck))
  def checkIf[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = cw.wrap(ConditionalCheckBuilder((r: R, s: Session) => condition(r, s), thenCheck))

  def regex(pattern: Expression[String])(implicit patterns: Patterns): RegexCheckBuilder[String] with RegexOfType = RegexCheckBuilder.regex(pattern, patterns)

  val bodyString = BodyStringCheckBuilder.BodyString

  val bodyBytes = BodyBytesCheckBuilder.BodyBytes

  def substring(pattern: Expression[String]) = new SubstringCheckBuilder(pattern)

  def xpath(path: Expression[String], namespaces: List[(String, String)] = Nil)(implicit saxon: Saxon, jdkXmlParsers: JdkXmlParsers) =
    new XPathCheckBuilder(path, namespaces, saxon, jdkXmlParsers)

  def css(selector: Expression[String])(implicit selectors: CssSelectors) =
    CssCheckBuilder.css(selector, None, selectors)
  def css(selector: Expression[String], nodeAttribute: String)(implicit selectors: CssSelectors) =
    CssCheckBuilder.css(selector, Some(nodeAttribute), selectors)
  def form(selector: Expression[String])(implicit selectors: CssSelectors) = css(selector).ofType[Map[String, Seq[String]]]

  def jsonPath(path: Expression[String])(implicit jsonPaths: JsonPaths) =
    JsonPathCheckBuilder.jsonPath(path, jsonPaths)

  def jsonpJsonPath(path: Expression[String])(implicit jsonPaths: JsonPaths) =
    JsonpJsonPathCheckBuilder.jsonpJsonPath(path, jsonPaths)

  val md5 = ChecksumCheckBuilder.Md5
  val sha1 = ChecksumCheckBuilder.Sha1

  val responseTimeInMillis = ResponseTimeCheckBuilder.ResponseTimeInMillis
}
