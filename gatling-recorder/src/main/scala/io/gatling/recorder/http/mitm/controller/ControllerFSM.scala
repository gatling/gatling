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

import scala.util.{ Failure, Success }

import io.gatling.recorder.http.Remote
import io.gatling.recorder.http.mitm.controller.Netty._

import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest

private[controller] object ControllerFSM {
  sealed trait Data

  object Data {
    final case class WaitingForClientChannelConnect(remote: Remote, pendingRequest: FullHttpRequest) extends Data
    final case class WaitingForProxyConnectResponse(remote: Remote, pendingRequest: FullHttpRequest, clientChannel: Channel) extends Data
    final case class Connected(remote: Remote, clientChannel: Channel) extends Data
    final case class Disconnected(remote: Remote) extends Data
  }
}

private[controller] abstract class ControllerFSM(clientBootstrap: Bootstrap) extends Controller with StrictLogging {

  private var behavior: Behavior = whenInit

  final def become(newBehavior: Behavior): Effect = _ => newBehavior

  override final def !(message: Controller.Message): Unit =
    behavior = behavior(message)(behavior)

  protected def whenInit: Behavior

  final val stay: Effect = identity

  final val stop: Effect = become { msg =>
    logger.info(s"Dropping msg '$msg' as actor is dead")
    stay
  }

  protected final def unhandled(msg: Controller.Message): Effect = {
    logger.error(s"Unhandled message $msg")
    stay
  }

  protected def connectedRemote(requestRemote: Remote): Remote

  protected def whenWaitingForClientChannelConnect(data: ControllerFSM.Data.WaitingForClientChannelConnect): Behavior

  protected def connectClientChannel(requestRemote: Remote, request: FullHttpRequest): Effect = {
    val remote = connectedRemote(requestRemote)

    logger.debug(s"Connecting to $remote")
    clientBootstrap
      .connect(remote.host, remote.port)
      .addScalaListener {
        case Success(channel) =>
          this ! Controller.Message.ClientChannelActive(channel)

        case Failure(t) =>
          logger.error(s"Connect failure with $remote", t)
          this ! Controller.Message.ClientChannelException(t)
      }

    become(whenWaitingForClientChannelConnect(ControllerFSM.Data.WaitingForClientChannelConnect(requestRemote, request)))
  }
}
