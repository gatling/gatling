/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import java.io.InputStream

import scala.annotation.implicitNotFound

import io.gatling.commons.validation.Validation
import io.gatling.core.check._
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
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.message._
import io.gatling.http.check.body._
import io.gatling.http.check.checksum._
import io.gatling.http.check.header._
import io.gatling.http.check.status._
import io.gatling.http.check.time._
import io.gatling.http.check.url._
import io.gatling.http.response.Response

import com.fasterxml.jackson.databind.JsonNode
import jodd.lagarto.dom.NodeSelector
import net.sf.saxon.s9api.XdmNode

trait HttpCheckSupport {

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def checkBuilder2HttpCheck[A, P, X](
      checkBuilder: CheckBuilder[A, P, X]
  )(implicit materializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def validatorCheckBuilder2HttpCheck[A, P, X](
      validatorCheckBuilder: ValidatorCheckBuilder[A, P, X]
  )(implicit CheckMaterializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def findCheckBuilder2HttpCheck[A, P, X](
      findCheckBuilder: FindCheckBuilder[A, P, X]
  )(implicit CheckMaterializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    findCheckBuilder.find.exists

  val currentLocation: FindCheckBuilder[CurrentLocationCheckType, String, String] = CurrentLocationCheckBuilder
  implicit val currentLocationCheckMaterializer: CheckMaterializer[CurrentLocationCheckType, HttpCheck, Response, String] = CurrentLocationCheckMaterializer

  def currentLocationRegex(
      pattern: Expression[String]
  )(implicit patterns: Patterns): MultipleFindCheckBuilder[CurrentLocationRegexCheckType, CharSequence, String] with CurrentLocationRegexOfType =
    CurrentLocationRegexCheckBuilder.currentLocationRegex(pattern, patterns)
  implicit val currentLocationRegexCheckMaterializer: CheckMaterializer[CurrentLocationRegexCheckType, HttpCheck, Response, CharSequence] =
    CurrentLocationRegexCheckMaterializer

  val status: FindCheckBuilder[HttpStatusCheckType, Response, Int] = HttpStatusCheckBuilder
  implicit val httpStatusCheckMaterializer: CheckMaterializer[HttpStatusCheckType, HttpCheck, Response, Response] = HttpStatusCheckMaterializer

  val header: Expression[String] => MultipleFindCheckBuilder[HttpHeaderCheckType, Response, String] = new HttpHeaderCheckBuilder(_)
  implicit val httpHeaderCheckMaterializer: CheckMaterializer[HttpHeaderCheckType, HttpCheck, Response, Response] = HttpHeaderCheckMaterializer

  def headerRegex(headerName: Expression[String], pattern: Expression[String])(
      implicit patterns: Patterns
  ): MultipleFindCheckBuilder[HttpHeaderRegexCheckType, Response, String] with HttpHeaderRegexOfType =
    HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern, patterns)
  implicit val httpHeaderRegexCheckMaterializer: CheckMaterializer[HttpHeaderRegexCheckType, HttpCheck, Response, Response] = HttpHeaderRegexCheckMaterializer

  implicit val httpBodyBytesCheckMaterializer: CheckMaterializer[BodyBytesCheckType, HttpCheck, Response, Array[Byte]] = HttpBodyBytesCheckMaterializer
  implicit val httpBodyStringCheckMaterializer: CheckMaterializer[BodyStringCheckType, HttpCheck, Response, String] = HttpBodyStringCheckMaterializer
  implicit val httpBodyStreamCheckMaterializer: CheckMaterializer[BodyStreamCheckType, HttpCheck, Response, () => InputStream] = HttpBodyStreamCheckMaterializer

  implicit val httpBodyRegexCheckMaterializer: CheckMaterializer[RegexCheckType, HttpCheck, Response, CharSequence] = HttpBodyRegexCheckMaterializer
  implicit val httpBodySubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, HttpCheck, Response, String] = HttpBodySubstringCheckMaterializer
  implicit def httpBodyXPathCheckMaterializer(implicit xmlParsers: XmlParsers): CheckMaterializer[XPathCheckType, HttpCheck, Response, Option[XdmNode]] =
    new HttpBodyXPathCheckMaterializer(xmlParsers)
  implicit def httpBodyCssCheckMaterializer(implicit selectors: CssSelectors): CheckMaterializer[CssCheckType, HttpCheck, Response, NodeSelector] =
    new HttpBodyCssCheckMaterializer(selectors)
  implicit def httpBodyJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, HttpCheck, Response, JsonNode] =
    new HttpBodyJsonPathCheckMaterializer(jsonParsers)
  implicit def httpBodyJsonpJsonPathCheckMaterializer(
      implicit jsonParsers: JsonParsers
  ): CheckMaterializer[JsonpJsonPathCheckType, HttpCheck, Response, JsonNode] = new HttpBodyJsonpCheckMaterializer(jsonParsers)
  implicit def httpBodyJmesPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, HttpCheck, Response, JsonNode] =
    new HttpBodyJmesPathCheckMaterializer(jsonParsers)
  implicit def httpBodyJsonpJmesPathCheckMaterializer(
      implicit jsonParsers: JsonParsers
  ): CheckMaterializer[JsonpJmesPathCheckType, HttpCheck, Response, JsonNode] = new HttpBodyJsonpCheckMaterializer(jsonParsers)

  implicit val httpMd5CheckMaterializer: CheckMaterializer[Md5CheckType, HttpCheck, Response, String] = HttpChecksumCheckMaterializer.Md5
  implicit val httpSha1CheckMaterializer: CheckMaterializer[Sha1CheckType, HttpCheck, Response, String] = HttpChecksumCheckMaterializer.Sha1

  implicit val httpResponseTimeCheckMaterializer: CheckMaterializer[ResponseTimeCheckType, HttpCheck, Response, ResponseTimings] =
    HttpResponseTimeCheckMaterializer

  implicit object HttpTypedConditionalCheckWrapper extends TypedConditionalCheckWrapper[Response, HttpCheck] {
    override def wrap(condition: (Response, Session) => Validation[Boolean], thenCheck: HttpCheck) =
      HttpCheck(ConditionalCheck(condition, thenCheck), thenCheck.scope)
  }

  implicit object HttpUntypedConditionalCheckWrapper extends UntypedConditionalCheckWrapper[HttpCheck] {
    override def wrap(condition: Expression[Boolean], thenCheck: HttpCheck): HttpCheck = {
      val typedCondition = (_: Response, session: Session) => condition(session)
      HttpCheck(ConditionalCheck(typedCondition, thenCheck), thenCheck.scope)
    }
  }
}
