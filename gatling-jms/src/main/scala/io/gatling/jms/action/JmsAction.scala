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
package io.gatling.jms.action

import javax.jms.Message

import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.jms.client.JmsClient
import io.gatling.jms.request._

import com.typesafe.scalalogging.Logger

trait JmsAction[T <: JmsClient] {

  protected def logger: Logger

  def attributes: JmsAttributes

  def client: T

  def sendMessage(session: Session)(f: (Message, Long) => Unit): Validation[Unit] = {

    val messageProperties = resolveProperties(attributes.messageProperties, session)

    val jmsType = resolveOptionalExpression(attributes.jmsType, session)

    val startDate = nowMillis

    val msg = jmsType.flatMap(jmsType =>
      messageProperties.flatMap { props =>
        attributes.message match {
          case BytesJmsMessage(bytes) => bytes(session).map(bytes => client.sendBytesMessage(bytes, props, jmsType))
          case MapJmsMessage(map) => map(session).map(map => client.sendMapMessage(map, props, jmsType))
          case ObjectJmsMessage(o) => o(session).map(o => client.sendObjectMessage(o, props, jmsType))
          case TextJmsMessage(txt) => txt(session).map(txt => client.sendTextMessage(txt, props, jmsType))
        }
      })

    msg.map {
      f(_, startDate)
    }
  }

  private def resolveProperties(
                                 properties: Map[Expression[String], Expression[Any]],
                                 session: Session
                               ): Validation[Map[String, Any]] =
    properties.foldLeft(Map.empty[String, Any].success) {
      case (resolvedProperties, (key, value)) =>
        for {
          key <- key(session)
          value <- value(session)
          resolvedProperties <- resolvedProperties
        } yield resolvedProperties + (key -> value)
    }

  def logMessage(text: String, msg: Message): Unit = {
    logger.debug(text)
    logger.trace(msg.toString)
  }
}
