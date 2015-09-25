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
package io.gatling.recorder.http.model

import io.gatling.http.util.BytesHelper

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http._

object SafeHttpRequest {

  def fromNettyRequest(nettyRequest: FullHttpRequest): SafeHttpRequest = {
    val request = SafeHttpRequest(
      nettyRequest.getProtocolVersion,
      nettyRequest.getMethod,
      nettyRequest.getUri,
      nettyRequest.headers,
      nettyRequest.trailingHeaders,
      BytesHelper.byteBufsToBytes(Seq(nettyRequest.content))
    )
    nettyRequest.release()
    request
  }
}

case class SafeHttpRequest(
  httpVersion:     HttpVersion,
  method:          HttpMethod,
  uri:             String,
  headers:         HttpHeaders,
  trailingHeaders: HttpHeaders,
  body:            Array[Byte]
) {

  def toNettyRequest: FullHttpRequest = {
    val request = new DefaultFullHttpRequest(httpVersion, method, uri, Unpooled.wrappedBuffer(body))
    request.headers.set(headers)
    request.trailingHeaders.set(trailingHeaders)
    request
  }
}
