/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.http.mitm.controller

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.{ OutgoingProxy, Remote }
import io.gatling.recorder.http.mitm.{ ClientHandler, Mitm, TrafficLogger }
import io.gatling.recorder.http.mitm.controller.Netty._
import io.gatling.recorder.http.ssl.{ SslClientContext, SslServerContext }
import io.gatling.recorder.util.HttpUtils

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ Channel, ChannelFutureListener }
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler

private[controller] final class SecuredWithProxyController(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    sslServerContext: SslServerContext,
    proxy: OutgoingProxy,
    trafficLogger: TrafficLogger,
    httpClientCodecFactory: () => HttpClientCodec,
    clock: Clock
) extends SecuredController(serverChannel, clientBootstrap) {
  private val proxyRemote = Remote(proxy.host, proxy.port)
  private val proxyBasicAuthHeader = proxy.credentials.map(HttpUtils.basicAuth)

  override protected def connectedRemote(requestRemote: Remote): Remote = proxyRemote

  override protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): Effect = {
    clientChannel.pipeline.addLast(Mitm.HandlerName.RecorderClient, new ClientHandler(this, serverChannel.id, trafficLogger, clock))

    // send connect request
    val connectRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, s"${remote.host}:${remote.port}")
    proxyBasicAuthHeader.foreach(header => connectRequest.headers.set(HttpHeaderNames.PROXY_AUTHORIZATION, header))
    clientChannel.writeAndFlush(connectRequest.filterSupportedEncodings)

    become(whenWaitingForProxyConnectResponse(ControllerFSM.Data.WaitingForProxyConnectResponse(remote, pendingRequest, clientChannel)))
  }

  private def whenWaitingForProxyConnectResponse(data: ControllerFSM.Data.WaitingForProxyConnectResponse): Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=WaitingForClientChannelConnect, closing")
      // FIXME what about client channel?
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.ClientChannelException(throwable) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      serverChannel.reply500AndClose()
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.ClientChannelInactive(inactiveClientChannelId) =>
      data.pendingRequest.release()
      if (inactiveClientChannelId == data.clientChannel.id) {
        logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client got closed, replying 500 and closing")
        serverChannel.reply500AndClose()
        // FIXME tell handlers to not notify of inactive state
        stop
      } else {
        // related to previous channel, ignoring
        stay
      }

    case Controller.Message.ResponseReceived(response) =>
      if (response.status == HttpResponseStatus.OK) {
        // the HttpClientCodec has to be regenerated, don't ask me why...
        data.clientChannel.pipeline.replace(Mitm.HandlerName.HttpCodec, Mitm.HandlerName.HttpCodec, httpClientCodecFactory())
        // install SslHandler on client channel
        val clientSslHandler = new SslHandler(SslClientContext.createSSLEngine(data.clientChannel.alloc, data.remote))
        data.clientChannel.pipeline.addFirst(Mitm.HandlerName.Ssl, clientSslHandler)

        if (data.pendingRequest.method == HttpMethod.CONNECT) {
          data.pendingRequest.release()

          // dealing with origin CONNECT from user-agent
          // install SslHandler on serverChannel with startTls = true so CONNECT response doesn't get encrypted
          val serverSslHandler = new SslHandler(sslServerContext.createSSLEngine(data.remote.host), true)
          serverChannel.pipeline.addFirst(Mitm.HandlerName.Ssl, serverSslHandler)
          serverChannel.writeAndFlush(response)
        } else {
          // dealing with client channel reconnect
          data.clientChannel.writeAndFlush(data.pendingRequest.filterSupportedEncodings)
        }

        become(whenConnected(ControllerFSM.Data.Connected(data.remote, data.clientChannel)))

      } else {
        serverChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        data.clientChannel.close()
        // FIXME tell handlers to not notify of inactive state
        stop
      }

    case msg => unhandled(msg)
  }
}
