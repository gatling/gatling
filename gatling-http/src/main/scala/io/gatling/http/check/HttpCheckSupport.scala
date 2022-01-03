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

package io.gatling.http.check

import java.io.InputStream

import scala.annotation.implicitNotFound

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
import io.gatling.core.session.Expression
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
  implicit def checkBuilder2HttpCheck[T, P](
      checkBuilder: CheckBuilder[T, P]
  )(implicit materializer: CheckMaterializer[T, HttpCheck, Response, P]): HttpCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def validate2HttpCheck[T, P, X](
      validate: CheckBuilder.Validate[T, P, X]
  )(implicit materializer: CheckMaterializer[T, HttpCheck, Response, P]): HttpCheck =
    validate.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def find2HttpCheck[T, P, X](
      find: CheckBuilder.Find[T, P, X]
  )(implicit materializer: CheckMaterializer[T, HttpCheck, Response, P]): HttpCheck =
    find.find.exists

  val currentLocation: CheckBuilder.Find[CurrentLocationCheckType, String, String] = CurrentLocationCheckBuilder
  implicit val currentLocationCheckMaterializer: CheckMaterializer[CurrentLocationCheckType, HttpCheck, Response, String] =
    CurrentLocationCheckMaterializer.Instance

  def currentLocationRegex(
      pattern: Expression[String]
  )(implicit patterns: Patterns): CheckBuilder.MultipleFind[CurrentLocationRegexCheckType, String, String] with CurrentLocationRegexOfType =
    CurrentLocationRegexCheckBuilder.currentLocationRegex(pattern, patterns)
  implicit val currentLocationRegexCheckMaterializer: CheckMaterializer[CurrentLocationRegexCheckType, HttpCheck, Response, String] =
    CurrentLocationRegexCheckMaterializer.Instance

  val status: CheckBuilder.Find[HttpStatusCheckType, Response, Int] = HttpStatusCheckBuilder
  implicit val httpStatusCheckMaterializer: CheckMaterializer[HttpStatusCheckType, HttpCheck, Response, Response] = HttpStatusCheckMaterializer.Instance

  def header(headerName: Expression[CharSequence]): CheckBuilder.MultipleFind[HttpHeaderCheckType, Response, String] = new HttpHeaderCheckBuilder(headerName)
  implicit val httpHeaderCheckMaterializer: CheckMaterializer[HttpHeaderCheckType, HttpCheck, Response, Response] = HttpHeaderCheckMaterializer.Instance

  def headerRegex(headerName: Expression[CharSequence], pattern: Expression[String])(implicit
      patterns: Patterns
  ): CheckBuilder.MultipleFind[HttpHeaderRegexCheckType, Response, String] with HttpHeaderRegexOfType =
    HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern, patterns)
  implicit val httpHeaderRegexCheckMaterializer: CheckMaterializer[HttpHeaderRegexCheckType, HttpCheck, Response, Response] =
    HttpHeaderRegexCheckMaterializer.Instance

  implicit val httpBodyBytesCheckMaterializer: CheckMaterializer[BodyBytesCheckType, HttpCheck, Response, Array[Byte]] = HttpBodyBytesCheckMaterializer.Instance
  implicit val httpBodyLengthCheckMaterializer: CheckMaterializer[BodyBytesCheckType, HttpCheck, Response, Int] = HttpBodyLengthCheckMaterializer.Instance
  implicit val httpBodyStringCheckMaterializer: CheckMaterializer[BodyStringCheckType, HttpCheck, Response, String] = HttpBodyStringCheckMaterializer.Instance
  implicit val httpBodyStreamCheckMaterializer: CheckMaterializer[BodyStreamCheckType, HttpCheck, Response, () => InputStream] =
    HttpBodyStreamCheckMaterializer.Instance

  implicit val httpBodyRegexCheckMaterializer: CheckMaterializer[RegexCheckType, HttpCheck, Response, String] = HttpBodyRegexCheckMaterializer.Instance
  implicit val httpBodySubstringCheckMaterializer: CheckMaterializer[SubstringCheckType, HttpCheck, Response, String] =
    HttpBodySubstringCheckMaterializer.Instance
  implicit val httpBodyXPathCheckMaterializer: CheckMaterializer[XPathCheckType, HttpCheck, Response, XdmNode] =
    HttpBodyXPathCheckMaterializer.Instance
  implicit def httpBodyCssCheckMaterializer(implicit selectors: CssSelectors): CheckMaterializer[CssCheckType, HttpCheck, Response, NodeSelector] =
    HttpBodyCssCheckMaterializer.instance(selectors)
  implicit def httpBodyJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, HttpCheck, Response, JsonNode] =
    HttpBodyJsonPathCheckMaterializer.instance(jsonParsers)
  implicit def httpBodyJsonpJsonPathCheckMaterializer(implicit
      jsonParsers: JsonParsers
  ): CheckMaterializer[JsonpJsonPathCheckType, HttpCheck, Response, JsonNode] = HttpBodyJsonpCheckMaterializer.instance(jsonParsers)
  implicit def httpBodyJmesPathCheckMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, HttpCheck, Response, JsonNode] =
    HttpBodyJmesPathCheckMaterializer.instance(jsonParsers)
  implicit def httpBodyJsonpJmesPathCheckMaterializer(implicit
      jsonParsers: JsonParsers
  ): CheckMaterializer[JsonpJmesPathCheckType, HttpCheck, Response, JsonNode] = HttpBodyJsonpCheckMaterializer.instance(jsonParsers)

  implicit val httpMd5CheckMaterializer: CheckMaterializer[Md5CheckType, HttpCheck, Response, String] = HttpChecksumCheckMaterializer.Md5
  implicit val httpSha1CheckMaterializer: CheckMaterializer[Sha1CheckType, HttpCheck, Response, String] = HttpChecksumCheckMaterializer.Sha1

  implicit val httpResponseTimeCheckMaterializer: CheckMaterializer[ResponseTimeCheckType, HttpCheck, Response, ResponseTimings] =
    HttpResponseTimeCheckMaterializer.Instance

  implicit val httpUntypedCheckIfMaker: UntypedCheckIfMaker[HttpCheck] = _.checkIf(_)

  implicit val httpTypedCheckIfMaker: TypedCheckIfMaker[Response, HttpCheck] = _.checkIf(_)
}
