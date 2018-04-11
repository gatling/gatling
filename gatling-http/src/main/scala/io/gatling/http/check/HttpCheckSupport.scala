/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.annotation.implicitNotFound

import io.gatling.commons.validation.Validation
import io.gatling.core.check._
import io.gatling.core.check.extractor.css.CssSelectors
import io.gatling.core.check.extractor.regex.Patterns
import io.gatling.core.check.extractor.xpath.XmlParsers
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.check.body._
import io.gatling.http.check.checksum.HttpChecksumCheckMaterializer
import io.gatling.http.check.header._
import io.gatling.http.check.status.{ HttpStatusCheckBuilder, HttpStatusCheckMaterializer }
import io.gatling.http.check.time.HttpResponseTimeCheckMaterializer
import io.gatling.http.check.url.{ CurrentLocationCheckBuilder, CurrentLocationCheckMaterializer, CurrentLocationRegexCheckBuilder, CurrentLocationRegexCheckMaterializer }
import io.gatling.http.response.Response

trait HttpCheckSupport {

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def checkBuilder2HttpCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def validatorCheckBuilder2HttpCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit CheckMaterializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for HTTP.")
  implicit def findCheckBuilder2HttpCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit CheckMaterializer: CheckMaterializer[A, HttpCheck, Response, P]): HttpCheck =
    findCheckBuilder.find.exists

  val currentLocation = CurrentLocationCheckBuilder.CurrentLocation
  implicit val currentLocationCheckMaterializer = CurrentLocationCheckMaterializer

  def currentLocationRegex(pattern: Expression[String])(implicit patterns: Patterns) =
    CurrentLocationRegexCheckBuilder.currentLocationRegex(pattern, patterns)
  implicit val currentLocationRegexCheckMaterializer = CurrentLocationRegexCheckMaterializer

  val status = HttpStatusCheckBuilder.Status
  implicit val httpStatusCheckMaterializer = HttpStatusCheckMaterializer

  val header = new HttpHeaderCheckBuilder(_)
  implicit val httpHeaderCheckMaterializer = HttpHeaderCheckMaterializer

  def headerRegex(headerName: Expression[String], pattern: Expression[String])(implicit patterns: Patterns) =
    HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern, patterns)
  implicit val httpHeaderRegexCheckMaterializer = HttpHeaderRegexCheckMaterializer

  implicit val httpBodyBytesCheckMaterializer = HttpBodyBytesCheckMaterializer
  implicit val httpBodyStringCheckMaterializer = HttpBodyStringCheckMaterializer

  implicit val httpBodyRegexCheckMaterializer = HttpBodyRegexCheckMaterializer
  implicit val httpBodySubstringCheckMaterializer = HttpBodySubstringCheckMaterializer
  implicit def httpBodyXPathCheckMaterializer(implicit xmlParsers: XmlParsers) = new HttpBodyXPathCheckMaterializer(xmlParsers)
  implicit def httpBodyCssCheckMaterializer(implicit selectors: CssSelectors) = new HttpBodyCssCheckMaterializer(selectors)
  implicit def httpBodyJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers) = new HttpBodyJsonPathCheckMaterializer(jsonParsers)
  implicit def httpBodyJsonpJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers) = new HttpBodyJsonpJsonPathCheckMaterializer(jsonParsers)

  implicit val httpMd5CheckMaterializer = HttpChecksumCheckMaterializer.Md5
  implicit val httpSha1CheckMaterializer = HttpChecksumCheckMaterializer.Sha1

  implicit val httpResponseTimeCheckMaterializer = HttpResponseTimeCheckMaterializer

  implicit object HttpTypedConditionalCheckWrapper extends TypedConditionalCheckWrapper[Response, HttpCheck] {
    override def wrap(condition: (Response, Session) => Validation[Boolean], thenCheck: HttpCheck) =
      HttpCheck(ConditionalCheck(condition, thenCheck), thenCheck.scope, thenCheck.responseBodyUsageStrategy)
  }

  implicit object HttpUntypedConditionalCheckWrapper extends UntypedConditionalCheckWrapper[HttpCheck] {
    override def wrap(condition: Expression[Boolean], thenCheck: HttpCheck) = {
      val typedCondition = (_: Response, session: Session) => condition(session)
      HttpCheck(ConditionalCheck(typedCondition, thenCheck), thenCheck.scope, thenCheck.responseBodyUsageStrategy)
    }
  }
}
