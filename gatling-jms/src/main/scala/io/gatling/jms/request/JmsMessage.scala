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

package io.gatling.jms.request

import java.io.{ Serializable => JSerializable }
import javax.jms.{ Message, Session => JmsSession }

import io.gatling.commons.validation.Validation
import io.gatling.core.session.{ Expression, Session }

sealed trait JmsMessage {
  private[jms] def jmsMessage(session: Session, jmsSession: JmsSession): Validation[Message]
}

final case class BytesJmsMessage(bytes: Expression[Array[Byte]]) extends JmsMessage {
  override private[jms] def jmsMessage(session: Session, jmsSession: JmsSession): Validation[Message] =
    bytes(session).map { b =>
      val message = jmsSession.createBytesMessage
      message.writeBytes(b)
      message
    }
}

final case class MapJmsMessage(map: Expression[Map[String, Any]]) extends JmsMessage {
  override private[jms] def jmsMessage(session: Session, jmsSession: JmsSession): Validation[Message] =
    map(session).map { m =>
      val message = jmsSession.createMapMessage
      m.foreach { case (key, value) => message.setObject(key, value) }
      message
    }
}

final case class ObjectJmsMessage(obj: Expression[JSerializable]) extends JmsMessage {
  override private[jms] def jmsMessage(session: Session, jmsSession: JmsSession): Validation[Message] =
    obj(session).map(jmsSession.createObjectMessage)
}

final case class TextJmsMessage(txt: Expression[String]) extends JmsMessage {
  override private[jms] def jmsMessage(session: Session, jmsSession: JmsSession): Validation[Message] =
    txt(session).map(jmsSession.createTextMessage)
}
