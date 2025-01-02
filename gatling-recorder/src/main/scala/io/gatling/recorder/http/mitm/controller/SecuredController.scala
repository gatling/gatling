/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.recorder.http.Remote
import io.gatling.recorder.http.mitm.controller.Netty._

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._

private[controller] abstract class SecuredController(serverChannel: Channel, clientBootstrap: Bootstrap) extends ControllerFSM(clientBootstrap) {

  protected def onClientChannelActive(clientChannel: Channel, pendingRequest: FullHttpRequest, remote: Remote): Effect

  override protected def whenInit: Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"serverChannel=${serverChannel.id} closed, state=Init, closing")
      stop

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"serverChannel=${serverChannel.id} received init request ${request.uri}, connecting")
      // CONNECT requests don't have a scheme
      connectClientChannel(Remote.fromAbsoluteUri("http://" + request.uri), request)

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
      onClientChannelActive(clientChannel, data.pendingRequest, data.remote)

    case Controller.Message.ClientChannelException(throwable) =>
      logger.debug(s"serverChannel=${serverChannel.id}, state=WaitingForClientChannelConnect, client connect failure, replying 500 and closing", throwable)
      // FIXME tell handlers to not notify of inactive state
      serverChannel.reply500AndClose()
      stop

    case Controller.Message.ClientChannelInactive(_) =>
      // such event can only be related to previous channel, ignoring
      stay

    case msg => unhandled(msg)
  }

  protected def whenConnected(data: ControllerFSM.Data.Connected): Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Connected state, closing")
      // FIXME tell handlers to not notify of inactive state
      data.clientChannel.close()
      stop

    case Controller.Message.ClientChannelInactive(inactiveClientChannelId) =>
      if (data.clientChannel.id == inactiveClientChannelId) {
        logger.debug(
          s"Server channel ${serverChannel.id} received ClientChannelInactive while in Connected state paired with ${data.clientChannel.id}, becoming disconnected"
        )
        become(whenDisconnected(ControllerFSM.Data.Disconnected(data.remote)))
      } else {
        // event from previous channel, ignoring
        stay
      }

    case Controller.Message.ClientChannelException(throwable) =>
      logger.debug(
        s"Server channel ${serverChannel.id} Client channel ${data.clientChannel.id} crashed while in Connected state, becoming disconnected",
        throwable
      )
      become(whenDisconnected(ControllerFSM.Data.Disconnected(data.remote)))

    case Controller.Message.ResponseReceived(response) =>
      logger.debug(s"Server channel ${serverChannel.id} received Response while in Connected state")
      serverChannel.writeAndFlush(response)
      stay

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request ${request.uri} while in Connected state")
      // DIFF FROM HTTP
      // https, no outgoing proxy => propagate request
      data.clientChannel.writeAndFlush(request)
      stay

    case msg => unhandled(msg)
  }

  private def whenDisconnected(data: ControllerFSM.Data.Disconnected): Behavior = {
    case Controller.Message.ServerChannelInactive =>
      logger.debug(s"Server channel ${serverChannel.id} was closed while in Disconnected state, closing")
      // FIXME what about client channel?
      stop

    case Controller.Message.RequestReceived(request) =>
      logger.debug(s"Server channel ${serverChannel.id} received Request while in Disconnected state, reconnecting")
      connectClientChannel(data.remote, request)

    case msg => unhandled(msg)
  }
}
