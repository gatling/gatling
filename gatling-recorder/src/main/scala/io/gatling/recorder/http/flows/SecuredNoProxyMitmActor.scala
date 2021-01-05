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
import io.gatling.recorder.http.{ ClientHandler, Mitm, TrafficLogger }
import io.gatling.recorder.http.Mitm._
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM._
import io.gatling.recorder.http.ssl.{ SslClientContext, SslServerContext }

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future

/**
 * Standard flow:
 * <ul>
 * <li>received CONNECT request with absolute url</li>
 * <li>connect to remote host</li>
 * <li>if connect is successful, reply 200/OK</li>
 * <li>install SslHandler on serverChannel</li>
 * <li>received request with relative url<li>
 * <li>propagate request to clientChannel<li>
 * </ul>
 *
 * @param serverChannel    the server channel connected to the user agent
 * @param clientBootstrap  the bootstrap to establish client channels with the remote
 * @param sslServerContext factory for SSLContexts
 * @param trafficLogger log the traffic
 */
class SecuredNoProxyMitmActor(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    sslServerContext: SslServerContext,
    trafficLogger: TrafficLogger,
    clock: Clock
) extends SecuredMitmActor(serverChannel, clientBootstrap, sslServerContext) {

  override protected def connectedRemote(requestRemote: Remote): Remote = requestRemote

  override protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): State = {
    val clientSslHandler = new SslHandler(SslClientContext.createSSLEngine(clientChannel.alloc, remote))
    clientChannel.pipeline.addLast(GatlingClientHandler, new ClientHandler(self, serverChannel.id, trafficLogger, clock))
    clientChannel.pipeline.addFirst(Mitm.SslHandlerName, clientSslHandler)

    // DIFF FROM HTTP
    if (pendingRequest.method == HttpMethod.CONNECT) {
      // request won't be propagated
      pendingRequest.release()

      // install SslHandler on serverChannel with startTls = true so CONNECT response doesn't get encrypted
      val serverSslHandler = new SslHandler(sslServerContext.createSSLEngine(remote.host), true)
      serverChannel.pipeline.addFirst(SslHandlerName, serverSslHandler)

      // reply 200/OK
      serverChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))

    } else {
      clientSslHandler
        .handshakeFuture()
        .addListener((future: Future[Channel]) => {
          if (future.isSuccess) {
            // propagate
            clientChannel.writeAndFlush(pendingRequest.filterSupportedEncodings)
          } else {
            throw future.cause
          }
        })
    }

    goto(Connected) using ConnectedData(remote, clientChannel)
  }
}
