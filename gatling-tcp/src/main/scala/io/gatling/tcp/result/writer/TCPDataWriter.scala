/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.tcp.result.writer

import io.gatling.core.result.writer._
import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress
import akka.io.Tcp._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.akka.BaseActor
import akka.actor.ActorDSL._
import akka.actor.ActorRef
import akka.io.Tcp.Connected
import io.gatling.core.result.writer.RunMessage
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.result.writer.RequestMessage
import io.gatling.core.result.writer.ShortScenarioDescription
import io.gatling.core.result.writer.GroupMessage
import akka.io.Tcp.CommandFailed
import io.gatling.tcp.util.JsonHelper
import scala.collection.mutable.ListBuffer

/**
 * Put gatling as a client and publish its results using TCP communication
 */
class TCPDataWriter extends DataWriter with StrictLogging {

  //cant start right the way and need to be started after the initialization of DataWriter, because if it fails
  // connecting, it will loop forever trying to connect and send terminate to parent actor
  lazy val msgSender = actor(context)(new TCPMessageSender(context.self))

  override def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription]) = {
  }

  override def onTerminateDataWriter() = {
    logger.info("Received flush. Closing connection.")
    context stop msgSender
    context stop self
  }

  override def onRequestMessage(request: RequestMessage) = {
    msgSender forward request
  }

  override def onGroupMessage(group: GroupMessage) = {
    msgSender forward group
  }

  override def onUserMessage(userMessage: UserMessage) = {
    msgSender forward userMessage
  }

  private class TCPMessageSender(listener: ActorRef) extends BaseActor {
    final val MESSAGE_DELIMITER = "\n"
    val buffer = new ListBuffer[Any]
    IO(Tcp) ! Connect(new InetSocketAddress(configuration.data.tcp.host, configuration.data.tcp.port))

    override def receive = {
      case msg @ (_: UserMessage | _: GroupMessage | _: RequestMessage) =>
        logger.warn(s"Received message ($msg) before connected. Buffering...")
        buffer += msg
      case CommandFailed(_: Connect) =>
        logger.warn("Can't connect. All messages will be ignored")
        listener ! Terminate
        context stop self
      case c @ Connected(remote, local) =>
        logger.info("Connected to " + c.remoteAddress)
        val connection = sender
        connection ! Register(self)
        logger.info("Sending previous received messages: " + buffer.size)
        buffer.foreach(msg => {
          val msgString: String = JsonHelper.toJson(Map[String, Any]("message_type" -> msg.getClass.getSimpleName, "message" -> msg))
          connection ! Write(ByteString(msgString + MESSAGE_DELIMITER))
        })
        buffer.clear
        logger.info("Sent")
        context become {
          case msg @ (_: UserMessage | _: GroupMessage | _: RequestMessage) =>
            val msgString: String = JsonHelper.toJson(Map[String, Any]("message_type" -> msg.getClass.getSimpleName, "message" -> msg))
            logger.trace(s"Sending message: $msgString")
            connection ! Write(ByteString(msgString + MESSAGE_DELIMITER))
          case data: ByteString =>
            connection ! Write(data)
          case CommandFailed(w: Write) =>
          // O/S buffer was full
          case Received(data) =>
            logger.warn(s"I am not supposed to receive this data: $data")
          case "close" =>
            connection ! Close
          case _: ConnectionClosed =>
            context stop self
        }
    }
  }

}
