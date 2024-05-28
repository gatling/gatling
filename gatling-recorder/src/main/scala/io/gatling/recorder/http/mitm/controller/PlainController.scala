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
import io.gatling.recorder.http.Remote
import io.gatling.recorder.http.mitm.{ ClientHandler, Mitm, TrafficLogger }
import io.gatling.recorder.http.mitm.controller.Netty._

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest

private[controller] abstract class PlainController(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    trafficLogger: TrafficLogger,
    clock: Clock
) extends ControllerFSM(clientBootstrap) {
  protected def propagatedRequest(originalRequest: FullHttpRequest): FullHttpRequest

  override protected def whenInit: Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=Init, closing")
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"serverChannel=${serverChannel.id} received init request ${request.uri}, connecting")
      connectClientChannel(Remote.fromAbsoluteUri(request.uri), request)

    case msg => unhandled(msg)
  }

  override protected def whenWaitingForClientChannelConnect(data: ControllerFSM.Data.WaitingForClientChannelConnect): Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=WaitingForClientChannelConnect, closing")
      // FIXME what about client channel?
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.ClientChannelActive(clientChannel) =>
      logger.debug(s"serverChannel=${serverChannel.id}, clientChannel=${clientChannel.id} active")
      clientChannel.pipeline.addLast(Mitm.HandlerName.RecorderClient, new ClientHandler(this, serverChannel.id, trafficLogger, clock))
      clientChannel.writeAndFlush(propagatedRequest(data.pendingRequest))
      become(whenConnected(ControllerFSM.Data.Connected(data.remote, clientChannel)))

    case Controller.Message.ClientChannelException(throwable) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      serverChannel.reply500AndClose()
      // FIXME tell handlers to not notify of inactive state
      stop

    case _: Controller.Message.ClientChannelInactive =>
      // such event can only be related to previous channel, ignoring
      stay

    case msg => unhandled(msg)
  }

  private def whenConnected(data: ControllerFSM.Data.Connected): Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Connected state, closing")
      data.clientChannel.close()
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.ClientChannelInactive(inactiveClientChannelId) =>
      if (data.clientChannel.id == inactiveClientChannelId) {
        logger.debug(
          s"Server channel ${serverChannel.id} received ClientChannelInactive while in Connected state paired with ${data.clientChannel.id}, becoming disconnected"
        )
        become(whenDisconnected)
      } else {
        // event from previous channel, ignoring
        stay
      }

    case Controller.Message.ClientChannelException(throwable) =>
      logger.debug(
        s"Server channel ${serverChannel.id} Client channel ${data.clientChannel.id} crashed while in Connected state, becoming disconnected",
        throwable
      )
      become(whenDisconnected)

    case Controller.Message.ResponseReceived(response) =>
      logger.debug(s"Server channel ${serverChannel.id} received Response while in Connected state")
      serverChannel.writeAndFlush(response)
      stay

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request ${request.uri} while in Connected state")

      val newRemote = Remote.fromAbsoluteUri(request.uri)

      if (newRemote == data.remote) {
        data.clientChannel.writeAndFlush(propagatedRequest(request))
        stay
      } else {
        // current clientChannel is connected to another host
        data.clientChannel.close()
        connectClientChannel(newRemote, request)
      }

    case msg => unhandled(msg)
  }

  private def whenDisconnected: Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Disconnected state, closing")
      // FIXME tell handlers to not notify of inactive state
      stop

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request while in Disconnected state, reconnecting")
      connectClientChannel(Remote.fromAbsoluteUri(request.uri), request)

    case msg => unhandled(msg)
  }
}
