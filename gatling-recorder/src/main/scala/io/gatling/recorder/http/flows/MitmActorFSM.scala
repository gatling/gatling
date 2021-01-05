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

import io.gatling.http.client.uri.Uri

import akka.actor.{ Actor, FSM }
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest

object Remote {

  def missingScheme(rawUri: String, https: Boolean): Option[String] =
    if (rawUri.startsWith("http")) {
      None
    } else if (https) {
      Some("https")
    } else {
      Some("http")
    }

  def fromAbsoluteUri(uriString: String): Remote = {
    val uri = Uri.create(uriString)
    Remote(uri.getHost, uri.getExplicitPort)
  }
}

final case class Remote(host: String, port: Int) {

  def makeAbsoluteUri(rawUri: String, https: Boolean): String = {
    val sb = new StringBuilder

    Remote.missingScheme(rawUri, https).foreach { scheme =>
      sb.append(scheme).append("://")
    }

    if (rawUri.isEmpty || rawUri.startsWith("/")) {
      sb.append(host)
      if ((https && port != 443) || (!https && port != 80)) {
        sb.append(":").append(port)
      }
    }
    sb.append(rawUri).toString
  }
}

object MitmActorFSM {

  // state
  case object Init extends MitmActorState
  case object WaitingForClientChannelConnect extends MitmActorState
  case object WaitingForProxyConnectResponse extends MitmActorState
  case object Connected extends MitmActorState
  case object Disconnected extends MitmActorState

  // data
  case object NoData extends MitmActorData
  final case class WaitingForClientChannelConnectData(remote: Remote, pendingRequest: FullHttpRequest) extends MitmActorData
  final case class WaitingForProxyConnectResponseData(remote: Remote, pendingRequest: FullHttpRequest, clientChannel: Channel) extends MitmActorData
  final case class ConnectedData(remote: Remote, clientChannel: Channel) extends MitmActorData
  final case class DisconnectedData(remote: Remote) extends MitmActorData
}

trait MitmActorFSM extends Actor with FSM[MitmActorState, MitmActorData]

sealed trait MitmActorState

sealed trait MitmActorData
