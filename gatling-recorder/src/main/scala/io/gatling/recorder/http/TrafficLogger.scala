/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.http

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

import io.gatling.http.client.uri.Uri
import io.gatling.netty.util.ByteBufUtils
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.flows.Remote
import io.gatling.recorder.model._

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.ChannelId
import io.netty.handler.codec.http.{ DefaultHttpHeaders, FullHttpRequest, FullHttpResponse, HttpHeaderValues, HttpMethod }

final case class Key(channelId: ChannelId)

class TrafficLogger(controller: RecorderController) extends StrictLogging {

  private val flyingRequests = new ConcurrentHashMap[ChannelId, HttpRequest]

  def logException(remote: Remote, throwable: Throwable): Unit =
    throwable match {
      case _: IOException =>
        val targetHostUri = Uri.create(s"https://${remote.host}:${remote.port}")
        logger.error(s"SslException, did you accept the certificate for $targetHostUri?")
        controller.secureConnection(targetHostUri)
      case _ =>
    }

  def logRequest(serverChannelId: ChannelId, request: FullHttpRequest, remote: Remote, https: Boolean, sendTimestamp: Long): Unit =
    // filter out CONNECT (if HAR was generated with a proxy such as Charles) and Upgrade requests (WebSockets)
    if (request.method != HttpMethod.CONNECT && !request.headers.contains(HttpHeaderValues.UPGRADE)) {
      import request._

      val requestEvent = HttpRequest(
        httpVersion = protocolVersion.text,
        method = method.name,
        uri = remote.makeAbsoluteUri(uri, https),
        headers = new DefaultHttpHeaders().add(headers).add(trailingHeaders),
        body = ByteBufUtils.byteBuf2Bytes(content),
        timestamp = sendTimestamp
      )

      flyingRequests.put(serverChannelId, requestEvent)
    }

  def logResponse(serverChannelId: ChannelId, response: FullHttpResponse, receiveTimestamp: Long): Unit =
    Option(flyingRequests.get(serverChannelId)).foreach { requestEvent =>
      flyingRequests.remove(serverChannelId)
      import response._

      val responseEvent = HttpResponse(
        status = status.code,
        statusText = status.reasonPhrase,
        headers = new DefaultHttpHeaders().add(headers).add(trailingHeaders),
        body = ByteBufUtils.byteBuf2Bytes(content),
        timestamp = receiveTimestamp
      )

      controller.receiveResponse(requestEvent, responseEvent)
    }

  def clear(serverChannelId: ChannelId): Unit =
    flyingRequests.remove(serverChannelId)
}
