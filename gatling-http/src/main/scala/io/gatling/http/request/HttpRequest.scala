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
import io.gatling.core.validation.Validation
import io.gatling.http.check.HttpCheck
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

case class HttpRequestConfig(
  checks: List[HttpCheck],
  responseTransformer: Option[PartialFunction[Response, Response]],
  extraInfoExtractor: Option[ExtraInfoExtractor],
  maxRedirects: Option[Int],
  throttled: Boolean,
  silent: Boolean,
  followRedirect: Boolean,
  discardResponseChunks: Boolean,
  protocol: HttpProtocol,
  explicitResources: List[HttpRequestDef])

object HttpRequestDef {

  def isSilent(config: HttpRequestConfig, ahcRequest: Request): Boolean = {

      def requestMadeSilentByProtocol: Boolean = config.protocol.requestPart.silentURI match {
        case Some(r) =>
          val uri = ahcRequest.getURI.toUrl
          r.pattern.matcher(uri).matches
        case None => false
      }

    config.silent || requestMadeSilentByProtocol
  }
}

case class HttpRequestDef(
    requestName: Expression[String],
    ahcRequest: Expression[Request],
    signatureCalculator: Option[SignatureCalculator],
    config: HttpRequestConfig) {

  def build(session: Session): Validation[HttpRequest] =
    for {
      rn <- requestName(session)
      httpRequest <- build(rn, session)
    } yield httpRequest

  def build(requestName: String, session: Session): Validation[HttpRequest] = {

      def sign(request: Request, signatureCalculator: Option[SignatureCalculator]): Request =
        signatureCalculator match {
          case Some(calculator) => new RequestBuilder(request).setSignatureCalculator(calculator).build()
          case None             => request
        }

    for {
      ahcRequest <- ahcRequest(session)
      newAhcRequest = sign(ahcRequest, signatureCalculator)
      newSilent = HttpRequestDef.isSilent(config, newAhcRequest)

    } yield HttpRequest(
      requestName,
      newAhcRequest,
      config.copy(silent = newSilent))
  }
}

case class HttpRequest(
  requestName: String,
  ahcRequest: Request,
  config: HttpRequestConfig)
