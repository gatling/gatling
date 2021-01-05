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

package io.gatling.recorder.http.flows

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.{ ClientHandler, Mitm, OutgoingProxy, TrafficLogger }
import io.gatling.recorder.http.Mitm._
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM.{ WaitingForProxyConnectResponse, _ }
import io.gatling.recorder.http.flows.MitmMessage._
import io.gatling.recorder.http.ssl.{ SslClientContext, SslServerContext }
import io.gatling.recorder.util.HttpUtils

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ Channel, ChannelFutureListener }
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler

/**
 * Standard flow:
 * <ul>
 * <li>received CONNECT request with absolute url but without scheme</li>
 * <li>connect to outgoing proxy</li>
 * <li>if connect is successful, send CONNECT request to proxy</li>
 * <li>if response is 200/OK, install SslHandler on clientChannel and serverChannel</li>
 * <li>propagate response to serverChannel<li>
 * <li>receive request with relative url</li>
 * <li>propagate request to clientChannel</li>
 * <li>receive response</li>
 * <li>propagate response to serverChannel</li>
 * </ul>
 *
 * @param serverChannel    the server channel connected to the user agent
 * @param clientBootstrap  the bootstrap to establish client channels with the remote
 * @param sslServerContext factory for SSLContexts
 * @param proxy the outgoing proxy
 * @param trafficLogger log the traffic
 * @param httpClientCodecFactory create new HttpClientCodecs
 */
class SecuredWithProxyMitmActor(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    sslServerContext: SslServerContext,
    proxy: OutgoingProxy,
    trafficLogger: TrafficLogger,
    httpClientCodecFactory: () => HttpClientCodec,
    clock: Clock
) extends SecuredMitmActor(serverChannel, clientBootstrap, sslServerContext) {

  private val proxyRemote = Remote(proxy.host, proxy.port)
  private val proxyBasicAuthHeader = proxy.credentials.map(HttpUtils.basicAuth)

  override protected def connectedRemote(requestRemote: Remote): Remote = proxyRemote

  override protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): State = {
    clientChannel.pipeline.addLast(GatlingClientHandler, new ClientHandler(self, serverChannel.id, trafficLogger, clock))

    // send connect request
    val connectRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, s"${remote.host}:${remote.port}")
    proxyBasicAuthHeader.foreach(header => connectRequest.headers.set(HttpHeaderNames.PROXY_AUTHORIZATION, header))
    clientChannel.writeAndFlush(connectRequest.filterSupportedEncodings)

    goto(WaitingForProxyConnectResponse) using WaitingForProxyConnectResponseData(remote, pendingRequest, clientChannel)
  }

  when(WaitingForProxyConnectResponse) {
    case Event(ServerChannelInactive, _) =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=WaitingForClientChannelConnect, closing")
      // FIXME what about client channel?
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(ClientChannelException(throwable), _) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      serverChannel.reply500AndClose()
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(ClientChannelInactive(inactiveClientChannelId), WaitingForProxyConnectResponseData(_, pendingRequest, clientChannel)) =>
      pendingRequest.release()
      if (inactiveClientChannelId == clientChannel.id) {
        logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client got closed, replying 500 and closing")
        serverChannel.reply500AndClose()
        // FIXME tell handlers to not notify of inactive state
        stop()
      } else {
        // related to previous channel, ignoring
        stay()
      }

    case Event(ResponseReceived(response), WaitingForProxyConnectResponseData(remote, pendingRequest, clientChannel)) =>
      if (response.status == HttpResponseStatus.OK) {
        // the HttpClientCodec has to be regenerated, don't ask me why...
        clientChannel.pipeline.replace(HttpCodecHandlerName, HttpCodecHandlerName, httpClientCodecFactory())
        // install SslHandler on client channel
        val clientSslHandler = new SslHandler(SslClientContext.createSSLEngine(clientChannel.alloc, remote))
        clientChannel.pipeline.addFirst(Mitm.SslHandlerName, clientSslHandler)

        if (pendingRequest.method == HttpMethod.CONNECT) {
          pendingRequest.release()

          // dealing with origin CONNECT from user-agent
          // install SslHandler on serverChannel with startTls = true so CONNECT response doesn't get encrypted
          val serverSslHandler = new SslHandler(sslServerContext.createSSLEngine(remote.host), true)
          serverChannel.pipeline.addFirst(SslHandlerName, serverSslHandler)
          serverChannel.writeAndFlush(response)

        } else {
          // dealing with client channel reconnect
          clientChannel.writeAndFlush(pendingRequest.filterSupportedEncodings)
        }

        goto(Connected) using ConnectedData(remote, clientChannel)

      } else {
        serverChannel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
        clientChannel.close()
        // FIXME tell handlers to not notify of inactive state
        stop()
      }
  }
}
