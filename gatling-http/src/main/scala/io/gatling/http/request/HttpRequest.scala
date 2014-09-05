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
/**
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
package io.gatling.http.request

import com.ning.http.client.{ RequestBuilder, SignatureCalculator, Request }

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation._
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

case class HttpRequestConfig(
  checks: List[HttpCheck],
  responseTransformer: Option[PartialFunction[Response, Response]],
  extraInfoExtractor: Option[ExtraInfoExtractor],
  maxRedirects: Option[Int],
  throttled: Boolean,
  silent: Option[Boolean],
  followRedirect: Boolean,
  discardResponseChunks: Boolean,
  protocol: HttpProtocol,
  explicitResources: List[HttpRequestDef])

case class HttpRequestDef(
    requestName: Expression[String],
    ahcRequest: Expression[Request],
    signatureCalculator: Option[Expression[SignatureCalculator]],
    config: HttpRequestConfig) {

  def build(session: Session): Validation[HttpRequest] =
    for {
      rn <- requestName(session)
      httpRequest <- build(rn, session)
    } yield httpRequest

  def build(requestName: String, session: Session): Validation[HttpRequest] = {

      def sign(request: Request, signatureCalculator: Option[Expression[SignatureCalculator]]): Validation[Request] =
        signatureCalculator match {
          case Some(calculatorExp) => calculatorExp(session).map(
            calculator => new RequestBuilder(request).setSignatureCalculator(calculator).build())
          case None => Success(request)
        }

    for {
      rawAhcRequest <- ahcRequest(session)
      signedAhcRequest <- sign(rawAhcRequest, signatureCalculator)

    } yield HttpRequest(requestName, signedAhcRequest, config)
  }
}

case class HttpRequest(requestName: String, ahcRequest: Request, config: HttpRequestConfig)
