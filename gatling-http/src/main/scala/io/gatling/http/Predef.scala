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
package io.gatling.http

import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response
import io.gatling.http.check.HttpCheckScope

import io.gatling.commons.validation.Success

import io.gatling.core.session._
import io.gatling.core.check.Check
import io.gatling.core.check.ConditionalCheck._

object Predef extends HttpDsl {

  type Request = org.asynchttpclient.Request
  type Response = io.gatling.http.response.Response

  implicit object HttpCheckWrapper extends CheckWrapper[Response, HttpCheck] {
    def wrap(check: Check[Response]): HttpCheck = new HttpCheck(check, HttpCheckScope.Body, None)
  }

  val HTTP_CONTENT_TYPE_HEADER_KEY: String = "Content-Type"
  val JSON_CONTENT_TYPE_VALUE: String = "application/json"
  
  def isJsonResponse(response: Response): Boolean = {
    response.isReceived && response.header(HTTP_CONTENT_TYPE_HEADER_KEY).exists { x => x.contains(JSON_CONTENT_TYPE_VALUE) }
  }

  def securedJsonCheck(check: Check[Response]): HttpCheck = checkIf((response: Response, session: Session) => Success(isJsonResponse(response)))(check)

}
