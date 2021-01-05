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

import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM._
import io.gatling.recorder.http.flows.MitmMessage._
import io.gatling.recorder.http.ssl.SslServerContext

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._

abstract class SecuredMitmActor(serverChannel: Channel, clientBootstrap: Bootstrap, sslServerContext: SslServerContext) extends MitmActor(clientBootstrap) {

  protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): State

  startWith(Init, NoData)

  when(Init) {
    case Event(ServerChannelInactive, NoData) =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=Init, closing")
      stop()

    case Event(RequestReceived(request), NoData) =>
      logger.debug(s"serverChannel=${serverChannel.id} received init request ${request.uri}, connecting")
      // CONNECT requests don't have a scheme
      connectClientChannel(Remote.fromAbsoluteUri("http://" + request.uri), request)
  }

  when(WaitingForClientChannelConnect) {
    case Event(ServerChannelInactive, _) =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=WaitingForClientChannelConnect, closing")
      // FIXME what about client channel?
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(ClientChannelActive(clientChannel), WaitingForClientChannelConnectData(remote, pendingRequest)) =>
      logger.debug(s"serverChannel=${serverChannel.id}, clientChannel=${clientChannel.id} active")
      onClientChannelActive(clientChannel, pendingRequest, remote)

    case Event(ClientChannelException(throwable), _) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      // FIXME tell handlers to not notify of inactive state
      serverChannel.reply500AndClose()
      stop()

    case Event(ClientChannelInactive(_), _) =>
      // such event can only be related to previous channel, ignoring
      stay()
  }

  when(Connected) {
    case Event(ServerChannelInactive, ConnectedData(_, clientChannel)) =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Connected state, closing")
      // FIXME tell handlers to not notify of inactive state
      clientChannel.close()
      stop()

    case Event(ClientChannelInactive(inactiveClientChannelId), ConnectedData(remote, clientChannel)) =>
      if (clientChannel.id == inactiveClientChannelId) {
        logger.debug(
          s"Server channel ${serverChannel.id} received ClientChannelInactive while in Connected state paired with ${clientChannel.id}, becoming disconnected"
        )
        goto(Disconnected) using DisconnectedData(remote)
      } else {
        // event from previous channel, ignoring
        stay()
      }

    case Event(ClientChannelException(throwable), ConnectedData(remote, clientChannel)) =>
      logger.debug(s"Server channel ${serverChannel.id} Client channel ${clientChannel.id} crashed while in Connected state, becoming disconnected", throwable)
      goto(Disconnected) using DisconnectedData(remote)

    case Event(ResponseReceived(response), _) =>
      logger.debug(s"Server channel ${serverChannel.id} received Response while in Connected state")
      serverChannel.writeAndFlush(response)
      stay()

    case Event(RequestReceived(request), ConnectedData(_, clientChannel)) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request ${request.uri} while in Connected state")
      // DIFF FROM HTTP
      // https, no outgoing proxy => propagate request
      clientChannel.writeAndFlush(request)
      stay()
  }

  when(Disconnected) {
    case Event(ServerChannelInactive, _) =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Disconnected state, closing")
      // FIXME what about client channel?
      stop()

    case Event(RequestReceived(request), DisconnectedData(remote)) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request while in Disconnected state, reconnecting")
      connectClientChannel(remote, request)
  }
}
