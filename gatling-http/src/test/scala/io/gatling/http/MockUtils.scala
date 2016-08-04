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

import io.gatling.core.CoreComponents
import io.gatling.core.session.Session
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.ahc.{ HttpEngine, ResponseProcessor }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.protocol.{ HttpComponents, HttpProtocol, HttpProtocolRequestPart }
import io.gatling.http.request.{ HttpRequest, HttpRequestConfig }

import io.netty.handler.codec.http.DefaultHttpHeaders
import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

object MockUtils extends MockitoSugar {

  def txTo(uri: String, session: Session, redirectCount: Int = 0, cache: Boolean = false) = {
    val protocol = mock[HttpProtocol]
    val coreComponents = mock[CoreComponents]
    val httpComponents = HttpComponents(protocol, mock[HttpEngine], mock[HttpCaches], mock[ResponseProcessor])
    val request = mock[Request]
    val requestPart = mock[HttpProtocolRequestPart]

    when(requestPart.cache) thenReturn cache
    when(requestPart.silentURI) thenReturn None
    when(requestPart.silentResources) thenReturn false
    when(request.getUri) thenReturn Uri.create(uri)
    when(request.getHeaders) thenReturn new DefaultHttpHeaders
    when(protocol.requestPart) thenReturn requestPart

    HttpTx(
      session,
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
          coreComponents = coreComponents,
          httpComponents = httpComponents,
          explicitResources = Nil
        )
      ),
      responseBuilderFactory = null,
      next = null,
      redirectCount = redirectCount
    )
  }
}
