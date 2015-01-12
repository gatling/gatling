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
package io.gatling.metrics.sender

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.io.{ IO, Tcp }
import akka.util.ByteString

private[metrics] class TcpSender(remote: InetSocketAddress) extends MetricsSender {

  import Tcp._

  IO(Tcp) ! Connect(remote)

  def receive = uninitialized

  val commandFailed = {
    case CommandFailed(cmd) => throw new RuntimeException(s"Command $cmd failed")
  }

  private def uninitialized: Receive = {
    case CommandFailed(_: Connect) =>
      logger.error(s"Graphite was unable to connect to $remote")
      context stop self
    case _: Connected =>
      unstashAll()
      val connection = sender()
      connection ! Register(self)
      context become connected(connection).orElse(commandFailed)
    case _ => stash()
  }

  override def sendByteString(connection: ActorRef, byteString: ByteString): Unit =
    connection ! Write(byteString)
}
