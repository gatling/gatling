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

import com.ning.http.client.uri.Uri
import io.gatling.core.session._
import io.gatling.core.validation.Validation
import io.gatling.http.HeaderNames
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.RequestBuilder._
import io.gatling.http.request.HttpRequest

object EmbeddedResource {

  val DefaultResourceChecks = List(DefaultHttpCheck)
}

sealed abstract class EmbeddedResource {

  def uri: Uri
  def acceptHeader: Expression[String]
  val url = uri.toString

  def toRequest(session: Session, protocol: HttpProtocol, throttled: Boolean): Validation[HttpRequest] = {

    val requestName = {
      val start = url.lastIndexOf('/') + 1
      if (start < url.length)
        url.substring(start, url.length)
      else
        "/"
    }

    val httpRequestDef = new Http(requestName.expression).get(uri).header(HeaderNames.Accept, acceptHeader) build (protocol, throttled)

    httpRequestDef.build(requestName, session)
  }
}

case class CssResource(uri: Uri) extends EmbeddedResource { val acceptHeader = CssHeaderHeaderValueExpression }

case class RegularResource(uri: Uri) extends EmbeddedResource { val acceptHeader = AllHeaderHeaderValueExpression }
