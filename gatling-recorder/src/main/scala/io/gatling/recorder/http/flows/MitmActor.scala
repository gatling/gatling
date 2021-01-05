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

import scala.util.{ Failure, Success }

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.{ OutgoingProxy, TrafficLogger }
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmActorFSM.{ WaitingForClientChannelConnect, WaitingForClientChannelConnectData }
import io.gatling.recorder.http.flows.MitmMessage.{ ClientChannelActive, ClientChannelException }
import io.gatling.recorder.http.ssl.SslServerContext

import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http.{ FullHttpRequest, HttpClientCodec }

object MitmActor {
  def apply(
      outgoingProxy: Option[OutgoingProxy],
      clientBootstrap: Bootstrap,
      sslServerContext: SslServerContext,
      trafficLogger: TrafficLogger,
      httpClientCodecFactory: () => HttpClientCodec,
      channel: Channel,
      https: Boolean,
      clock: Clock
  ): MitmActor =
    if (https) {
      outgoingProxy match {
        case Some(proxy) => new SecuredWithProxyMitmActor(channel, clientBootstrap, sslServerContext, proxy, trafficLogger, httpClientCodecFactory, clock)
        case _           => new SecuredNoProxyMitmActor(channel, clientBootstrap, sslServerContext, trafficLogger, clock)
      }
    } else {
      outgoingProxy match {
        case Some(proxy) => new PlainWithProxyMitmActor(channel, clientBootstrap, proxy, trafficLogger, clock)
        case _           => new PlainNoProxyMitmActor(channel, clientBootstrap, trafficLogger, clock)
      }
    }
}

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
