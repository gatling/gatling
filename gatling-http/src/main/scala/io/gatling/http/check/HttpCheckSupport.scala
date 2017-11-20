/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
import io.gatling.http.check.checksum.HttpChecksumProvider
import io.gatling.http.check.header._
import io.gatling.http.check.status.{ HttpStatusCheckBuilder, HttpStatusProvider }
import io.gatling.http.check.time.HttpResponseTimeProvider
import io.gatling.http.check.url.{ CurrentLocationCheckBuilder, CurrentLocationProvider, CurrentLocationRegexCheckBuilder, CurrentLocationRegexProvider }
import io.gatling.http.response.Response

trait HttpCheckSupport {

  @implicitNotFound("Could not find a CheckProtocolProvider. This check might not be a valid HTTP one.")
  implicit def checkBuilder2HttpCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, HttpCheck, Response, P]): HttpCheck =
    checkBuilder.build(provider)

  @implicitNotFound("Could not find a CheckProtocolProvider. This check might not be a valid HTTP one.")
  implicit def validatorCheckBuilder2HttpCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, HttpCheck, Response, P]): HttpCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckProtocolProvider. This check might not be a valid HTTP one.")
  implicit def findCheckBuilder2HttpCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, HttpCheck, Response, P]): HttpCheck =
    findCheckBuilder.find.exists

  val currentLocation = CurrentLocationCheckBuilder.CurrentLocation
  implicit val currentLocationProvider = CurrentLocationProvider

  def currentLocationRegex(pattern: Expression[String])(implicit patterns: Patterns) =
    CurrentLocationRegexCheckBuilder.currentLocationRegex(pattern, patterns)
  implicit val currentLocationRegexProvider = CurrentLocationRegexProvider

  val status = HttpStatusCheckBuilder.Status
  implicit val httpStatusProvider = HttpStatusProvider

  val header = new HttpHeaderCheckBuilder(_)
  implicit val httpHeaderProvider = HttpHeaderProvider

  def headerRegex(headerName: Expression[String], pattern: Expression[String])(implicit patterns: Patterns) =
    HttpHeaderRegexCheckBuilder.headerRegex(headerName, pattern, patterns)
  implicit val httpHeaderRegexProvider = HttpHeaderRegexProvider

  implicit val httpBodyBytesProvider = HttpBodyBytesProvider
  implicit val httpBodyStringProvider = HttpBodyStringProvider

  implicit val httpBodyRegexProvider = HttpBodyRegexProvider
  implicit val httpBodySubstringProvider = HttpBodySubstringProvider
  implicit def httpBodyXPathProvider(implicit xmlParsers: XmlParsers) = new HttpBodyXPathProvider(xmlParsers)
  implicit def httpBodyCssProvider(implicit selectors: CssSelectors) = new HttpBodyCssProvider(selectors)
  implicit def httpBodyJsonPathProvider(implicit jsonParsers: JsonParsers) = new HttpBodyJsonPathProvider(jsonParsers)
  implicit def httpBodyJsonpJsonPathProvider(implicit jsonParsers: JsonParsers) = new HttpBodyJsonpJsonPathProvider(jsonParsers)

  implicit val httpMd5Provider = HttpChecksumProvider.Md5
  implicit val httpSha1Provider = HttpChecksumProvider.Sha1

  implicit val httpResponseTimeProvider = HttpResponseTimeProvider

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
