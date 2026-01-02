/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.recorder.http.Remote
import io.gatling.recorder.http.mitm.{ ClientHandler, Mitm, TrafficLogger }
import io.gatling.recorder.http.mitm.controller.Netty._
import io.gatling.recorder.http.ssl.{ SslClientContext, SslServerContext }

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future

private[controller] final class SecuredNoProxyController(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    sslServerContext: SslServerContext,
    trafficLogger: TrafficLogger,
    clock: Clock
) extends SecuredController(serverChannel, clientBootstrap) {
  override protected def connectedRemote(requestRemote: Remote): Remote = requestRemote

  override protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): Effect = {
    val clientSslHandler = new SslHandler(SslClientContext.createSSLEngine(clientChannel.alloc, remote))
    clientChannel.pipeline.addLast(Mitm.HandlerName.RecorderClient, new ClientHandler(this, serverChannel.id, trafficLogger, clock))
    clientChannel.pipeline.addFirst(Mitm.HandlerName.Ssl, clientSslHandler)

    // DIFF FROM HTTP
    if (pendingRequest.method == HttpMethod.CONNECT) {
      // request won't be propagated
      pendingRequest.release()

      // install SslHandler on serverChannel with startTls = true so CONNECT response doesn't get encrypted
      val serverSslHandler = new SslHandler(sslServerContext.createSSLEngine(remote.host), true)
      serverChannel.pipeline.addFirst(Mitm.HandlerName.Ssl, serverSslHandler)

      // reply 200/OK
      serverChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
    } else {
      clientSslHandler
        .handshakeFuture()
        .addListener { (future: Future[Channel]) =>
          if (future.isSuccess) {
            // propagate
            clientChannel.writeAndFlush(pendingRequest.filterSupportedEncodings)
          } else {
            throw future.cause
          }
        }
    }

    become(whenConnected(ControllerFSM.Data.Connected(remote, clientChannel)))
  }
}
