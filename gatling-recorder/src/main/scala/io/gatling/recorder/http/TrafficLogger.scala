/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.ClockSingleton.nowMillis
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.Remote
import io.gatling.recorder.http.model.{ SafeHttpRequest, SafeHttpResponse, TimedHttpRequest }

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{ FullHttpRequest, FullHttpResponse, HttpMethod }
import org.asynchttpclient.uri.Uri

class TrafficLogger(controller: RecorderController) extends StrictLogging {

  private val flyingRequests = new ConcurrentHashMap[ChannelId, TimedHttpRequest]

  private case class Key(channelId: ChannelId)

  def logException(remote: Remote, throwable: Throwable): Unit =
    throwable match {
      case e: IOException =>
        val targetHostUri = Uri.create(s"https://${remote.host}:${remote.port}")
        logger.error(s"SslException, did you accept the certificate for $targetHostUri?")
        controller.secureConnection(targetHostUri)
      case _ =>
    }

  def logRequest(serverChannelId: ChannelId, request: FullHttpRequest, remote: Remote, https: Boolean): Unit =
    if (request.getMethod != HttpMethod.CONNECT) {
      val now = nowMillis
      val safeRequest = SafeHttpRequest.fromNettyRequest(request, remote, https)
      controller.receiveRequest(safeRequest)
      flyingRequests.put(serverChannelId, TimedHttpRequest(safeRequest, now))
    }

  def logResponse(serverChannelId: ChannelId, response: FullHttpResponse): Unit =
    Option(flyingRequests.get(serverChannelId)).foreach { timedHttpRequest =>
      flyingRequests.remove(serverChannelId)
      val safeResponse = SafeHttpResponse.fromNettyResponse(response)
      controller.receiveResponse(timedHttpRequest, safeResponse)
    }

  def clear(serverChannelId: ChannelId): Unit =
    flyingRequests.remove(serverChannelId)
}
