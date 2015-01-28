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

import akka.actor.ActorRef
import akka.io.{ Udp, IO }
import io.gatling.core.akka.BaseActor

class UdpServer extends BaseActor {

  import Udp._

  IO(Udp) ! Bind(self, new InetSocketAddress("localhost", 2003))

  def receive = {
    case Bound(local) => context.become(bound(sender()))
  }

  def bound(socket: ActorRef): Receive = {
    case Received(data, _) => // Do nothing upon receive
    case Unbind            => socket ! Unbind
    case Unbound           => context stop self
  }
}
