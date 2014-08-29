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
package io.gatling.http

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

import com.ning.http.client.Request
import com.ning.http.client.uri.UriComponents

import io.gatling.core.session.Session
import io.gatling.http.ahc.HttpTx
import io.gatling.http.config.{ HttpProtocolRequestPart, HttpProtocol }
import io.gatling.http.request.{ HttpRequestConfig, HttpRequest }

object MockUtils extends MockitoSugar {

  def txTo(uri: String, session: Session, redirectCount: Int = 0, cache: Boolean = false) = {
    val protocol = mock[HttpProtocol]
    val request = mock[Request]
    val requestPart = mock[HttpProtocolRequestPart]

    when(requestPart.cache) thenReturn cache
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn false
    when(request.getURI) thenReturn UriComponents.create(uri)
    when(protocol.requestPart) thenReturn requestPart

    HttpTx(session,
      request = HttpRequest(
        requestName = "mockHttpTx",
        ahcRequest = request,
        config = HttpRequestConfig(
          checks = Nil,
          responseTransformer = None,
          extraInfoExtractor = None,
          maxRedirects = Some(10),
          throttled = false,
          silent = None,
          followRedirect = true,
          discardResponseChunks = true,
          protocol = protocol,
          explicitResources = Nil)),
      responseBuilderFactory = null,
      next = null,
      redirectCount = redirectCount)
  }
}
