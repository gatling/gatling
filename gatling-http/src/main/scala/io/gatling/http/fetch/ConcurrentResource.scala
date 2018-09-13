/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.http.fetch

import io.gatling.commons.validation.Validation
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HeaderNames
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.RequestBuilder._
import io.gatling.http.request.HttpRequest

object ConcurrentResource {

  val DefaultResourceChecks = List(DefaultHttpCheck)
}

sealed abstract class ConcurrentResource {

  def uri: Uri
  def acceptHeader: Expression[String]
  val url: String = uri.toString

  def toRequest(session: Session, httpCaches: HttpCaches, httpProtocol: HttpProtocol, throttled: Boolean, configuration: GatlingConfiguration): Validation[HttpRequest] = {
    val requestName = httpProtocol.responsePart.inferredHtmlResourcesNaming(uri)
    val httpRequestDef = Http(requestName.expressionSuccess).get(uri).header(HeaderNames.Accept, acceptHeader).build(httpCaches, httpProtocol, throttled, configuration)
    httpRequestDef.build(requestName, session)
  }
}

case class CssResource(uri: Uri) extends ConcurrentResource { override val acceptHeader: Expression[String] = AcceptCssHeaderValueExpression }

case class BasicResource(uri: Uri) extends ConcurrentResource { override val acceptHeader: Expression[String] = AcceptAllHeaderValueExpression }
