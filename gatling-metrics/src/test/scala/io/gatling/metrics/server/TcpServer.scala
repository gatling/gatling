/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics.server

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8

import akka.actor.Props
import akka.io.{ IO, Tcp }

import io.gatling.core.akka.BaseActor

class TcpServer extends BaseActor {

  var receivedCount = 0

  import Tcp._

  IO(Tcp) ! Bind(self, new InetSocketAddress(2003))

  logger.debug("Starting local TCP server on port 2003")

  def receive = {
    case b: Bound               => logger.debug("Server is now bound")
    case CommandFailed(_: Bind) => context stop self
    case c: Connected =>
      sender() ! Register(self)
      context become connected
  }

  def connected: Receive = {
    case Received(data) =>
      receivedCount += 1
      logger.debug(s"Received ${data.decodeString(UTF_8.name())}")
    case PeerClosed => context stop self
  }
}
