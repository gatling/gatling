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
package io.gatling.http.fetch

import io.gatling.commons.validation.Validation
import io.gatling.core.CoreComponents
import io.gatling.core.session._
import io.gatling.http.HeaderNames
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.RequestBuilder._
import io.gatling.http.request.HttpRequest

import org.asynchttpclient.uri.Uri

object EmbeddedResource {

  val DefaultResourceChecks = List(DefaultHttpCheck)
}

sealed abstract class EmbeddedResource {

  def uri: Uri
  def acceptHeader: Expression[String]
  val url = uri.toString

  def toRequest(session: Session, coreComponents: CoreComponents, httpComponents: HttpComponents, throttled: Boolean): Validation[HttpRequest] = {

    val requestName = {
      val start = url.lastIndexOf('/') + 1
      if (start < url.length)
        url.substring(start, url.length)
      else
        "/"
    }

    val http = new Http(requestName.expressionSuccess)
    val httpRequestDef = http.get(uri).header(HeaderNames.Accept, acceptHeader).build(coreComponents, httpComponents, throttled)

    httpRequestDef.build(requestName, session)
  }
}

case class CssResource(uri: Uri) extends EmbeddedResource { val acceptHeader = CssHeaderHeaderValueExpression }

case class RegularResource(uri: Uri) extends EmbeddedResource { val acceptHeader = AllHeaderHeaderValueExpression }
