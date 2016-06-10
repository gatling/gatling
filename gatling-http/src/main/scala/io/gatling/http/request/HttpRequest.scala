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
package io.gatling.http.request

import io.gatling.commons.validation.Validation
import io.gatling.core.CoreComponents
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.response.Response

import org.asynchttpclient.Request

case class HttpRequestConfig(
  checks:                List[HttpCheck],
  responseTransformer:   Option[PartialFunction[Response, Response]],
  extraInfoExtractor:    Option[ExtraInfoExtractor],
  maxRedirects:          Option[Int],
  throttled:             Boolean,
  silent:                Option[Boolean],
  followRedirect:        Boolean,
  discardResponseChunks: Boolean,
  coreComponents:        CoreComponents,
  httpComponents:        HttpComponents,
  explicitResources:     List[HttpRequestDef]
)

case class HttpRequestDef(
    requestName: Expression[String],
    ahcRequest:  Expression[Request],
    config:      HttpRequestConfig
) {

  def build(requestName: String, session: Session): Validation[HttpRequest] =
    ahcRequest(session).map(HttpRequest(requestName, _, config))
}

case class HttpRequest(requestName: String, ahcRequest: Request, config: HttpRequestConfig)
