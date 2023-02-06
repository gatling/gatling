/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.http.request

import java.nio.charset.Charset

import io.gatling.commons.validation.Validation
import io.gatling.core.check.ChecksumAlgorithm
import io.gatling.core.session._
import io.gatling.http.ResponseTransformer
import io.gatling.http.check.HttpCheck
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpProtocol

final case class HttpRequestConfig(
    checks: List[HttpCheck],
    responseTransformer: Option[ResponseTransformer],
    throttled: Boolean,
    silent: Option[Boolean],
    followRedirect: Boolean,
    checksumAlgorithms: List[ChecksumAlgorithm],
    storeBodyParts: Boolean,
    defaultCharset: Charset,
    explicitResources: List[HttpRequestDef],
    httpProtocol: HttpProtocol
)

final case class HttpRequestDef(
    requestName: Expression[String],
    clientRequest: Expression[Request],
    requestConfig: HttpRequestConfig
) {
  def build(session: Session): Validation[HttpRequest] =
    clientRequest(session).map(request => HttpRequest(request.getName, request, requestConfig))
}

final case class HttpRequest(requestName: String, clientRequest: Request, requestConfig: HttpRequestConfig) {
  def isSilent(root: Boolean): Boolean =
    requestConfig.silent match {
      case Some(silent) => silent
      case _ =>
        val requestPart = requestConfig.httpProtocol.requestPart
        requestPart.silentUri.exists(_.matcher(clientRequest.getUri.toUrl).matches) || // silent because matches protocol's silentUri
        (!root && requestPart.silentResources) // silent because resources are silent
    }
}
