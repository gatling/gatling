/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.util.{ Failure, Success }

import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM.{ WaitingForClientChannelConnect, WaitingForClientChannelConnectData }
import io.gatling.recorder.http.flows.MitmMessage.{ ClientChannelActive, ClientChannelException }

import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.handler.codec.http.FullHttpRequest

abstract class MitmActor(clientBootstrap: Bootstrap) extends MitmActorFSM with StrictLogging {

  protected def connectedRemote(requestRemote: Remote): Remote

  protected def connectClientChannel(requestRemote: Remote, request: FullHttpRequest): State = {

    val remote = connectedRemote(requestRemote)

    logger.debug(s"Connecting to $remote")
    clientBootstrap
      .connect(remote.host, remote.port)
      .addScalaListener {
        case Success(channel) =>
          self ! ClientChannelActive(channel)

        case Failure(t) =>
          logger.error(s"Connect failure with $remote", t)
          self ! ClientChannelException(t)
      }

    goto(WaitingForClientChannelConnect) using WaitingForClientChannelConnectData(requestRemote, request)
  }

  override def unhandled(msg: Any): Unit =
    logger.error(s"Unhandled message $msg")
}
