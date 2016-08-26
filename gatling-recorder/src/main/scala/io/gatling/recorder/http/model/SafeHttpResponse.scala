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
package io.gatling.recorder.http.model

import scala.collection.JavaConversions._

import io.gatling.commons.util.StringHelper._

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http._
import org.asynchttpclient.netty.util.ByteBufUtils

object SafeHttpResponse {

  def fromNettyResponse(nettyResponse: FullHttpResponse): SafeHttpResponse = {
    val response = SafeHttpResponse(
      nettyResponse.getProtocolVersion,
      nettyResponse.getStatus,
      nettyResponse.headers(),
      nettyResponse.trailingHeaders(),
      ByteBufUtils.byteBuf2Bytes(nettyResponse.content)
    )
    nettyResponse.release()
    response
  }
}

case class SafeHttpResponse(
    httpVersion:     HttpVersion,
    status:          HttpResponseStatus,
    headers:         HttpHeaders,
    trailingHeaders: HttpHeaders,
    body:            Array[Byte]
) {

  def toNettyResponse: FullHttpResponse = {
    val response = new DefaultFullHttpResponse(httpVersion, status, Unpooled.wrappedBuffer(body))
    response.headers.set(headers)
    response.trailingHeaders.set(trailingHeaders)
    response
  }

  def summary: String =
    s"""$httpVersion $status
       |${(headers ++ trailingHeaders).map { entry => s"${entry.getKey}: ${entry.getValue}" }.mkString(Eol)}""".stripMargin
}
