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
package io.gatling.http.fetch

import java.net.URI

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.action.HttpRequestActionBuilder
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.Http
import io.gatling.http.request.HttpRequest

object EmbeddedResource {

  val DefaultResourceChecks = List(HttpRequestActionBuilder.DefaultHttpCheck)

  val MockSession = Session("foo", "bar")
}

sealed abstract class EmbeddedResource {

  def uri: URI
  val url = uri.toString

  def toRequest(protocol: HttpProtocol, throttled: Boolean): Validation[HttpRequest] = {

    val requestNameExpression: Expression[String] = {

      val requestName = {
        val start = url.lastIndexOf('/') + 1
        if (start < url.length)
          url.substring(start, url.length)
        else
          "/"
      }

      _ => requestName.success
    }

    val httpRequestDef = new Http(requestNameExpression).get(uri).build(protocol, throttled)

    // for now, no better way to build a request than reusing HttpRequestBaseBuilder and passing a mock session
    httpRequestDef.build(EmbeddedResource.MockSession)
  }
}

case class CssResource(uri: URI) extends EmbeddedResource

case class RegularResource(uri: URI) extends EmbeddedResource
