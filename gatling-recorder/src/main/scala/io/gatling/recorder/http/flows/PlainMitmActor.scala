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
import io.gatling.recorder.http.{ ClientHandler, TrafficLogger }
import io.gatling.recorder.http.Mitm._
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM._
import io.gatling.recorder.http.flows.MitmMessage._

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest

abstract class PlainMitmActor(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    trafficLogger: TrafficLogger,
    clock: Clock
) extends MitmActor(clientBootstrap) {

  protected def propagatedRequest(originalRequest: FullHttpRequest): FullHttpRequest

  startWith(Init, NoData)

  when(Init) {
    case Event(ServerChannelInactive, NoData) =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=Init, closing")
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(RequestReceived(request), NoData) =>
      logger.debug(s"serverChannel=${serverChannel.id} received init request ${request.uri}, connecting")
      connectClientChannel(Remote.fromAbsoluteUri(request.uri), request)
  }

  when(WaitingForClientChannelConnect) {
    case Event(ServerChannelInactive, _) =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=WaitingForClientChannelConnect, closing")
      // FIXME what about client channel?
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(ClientChannelActive(clientChannel), WaitingForClientChannelConnectData(remote, pendingRequest)) =>
      logger.debug(s"serverChannel=${serverChannel.id}, clientChannel=${clientChannel.id} active")
      clientChannel.pipeline.addLast(GatlingClientHandler, new ClientHandler(self, serverChannel.id, trafficLogger, clock))
      clientChannel.writeAndFlush(propagatedRequest(pendingRequest))
      goto(Connected) using ConnectedData(remote, clientChannel)

    case Event(ClientChannelException(throwable), _) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      serverChannel.reply500AndClose()
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(ClientChannelInactive(inactiveClientChannelId), _) =>
      // such event can only be related to previous channel, ignoring
      stay()
  }

  when(Connected) {
    case Event(ServerChannelInactive, ConnectedData(_, clientChannel)) =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Connected state, closing")
      clientChannel.close()
      // FIXME tell handlers to not notify of inactive state
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

    case Event(RequestReceived(request), ConnectedData(remote, clientChannel)) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request ${request.uri} while in Connected state")

      val newRemote = Remote.fromAbsoluteUri(request.uri)

      if (newRemote == remote) {
        clientChannel.writeAndFlush(propagatedRequest(request))
        stay()
      } else {
        // current clientChannel is connected to another host
        clientChannel.close()
        connectClientChannel(newRemote, request)
      }
  }

  when(Disconnected) {
    case Event(ServerChannelInactive, _) =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Disconnected state, closing")
      // FIXME tell handlers to not notify of inactive state
      stop()

    case Event(RequestReceived(request), _) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request while in Disconnected state, reconnecting")
      connectClientChannel(Remote.fromAbsoluteUri(request.uri), request)
  }
}
