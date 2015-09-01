/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.recorder.http.handler

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, HttpRequest }

private[handler] trait ScalaChannelHandler {

  implicit def function2ChannelFutureListener(thunk: ChannelFuture => Any) = new ChannelFutureListener {
    def operationComplete(future: ChannelFuture): Unit = thunk(future)
  }

  def copyRequestWithNewUri(request: HttpRequest, uri: String): HttpRequest = {
    val newRequest = new DefaultHttpRequest(request.getProtocolVersion, request.getMethod, uri)
    newRequest.setChunked(request.isChunked)
    newRequest.setContent(request.getContent)
    for (header <- request.headers.entries) newRequest.headers.add(header.getKey, header.getValue)
    newRequest
  }
}
