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

package io.gatling.jms.request

import java.io.{ Serializable => JSerializable }
import javax.jms.Message

import io.gatling.commons.validation.Validation
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{ Expression, ExpressionSuccessWrapper, Session }
import io.gatling.jms.JmsCheck
import io.gatling.jms.action.{ RequestReplyBuilder, SendBuilder }

import com.softwaremill.quicklens._

final class JmsDslBuilderBase(requestName: Expression[String]) {
  def send: SendDslBuilder.Queue = new SendDslBuilder.Queue(requestName)
  def requestReply: RequestReplyDslBuilder.Queue = new RequestReplyDslBuilder.Queue(requestName)
}

object SendDslBuilder {
  final class Queue(requestName: Expression[String]) {
    def queue(name: Expression[String]): Message = destination(JmsDestination.Queue(name))
    def destination(destination: JmsDestination): Message = new Message(requestName, destination)
  }

  final class Message(requestName: Expression[String], destination: JmsDestination) {
    def textMessage(text: Expression[String]): SendDslBuilder = message(TextJmsMessage(text))
    def bytesMessage(bytes: Expression[Array[Byte]]): SendDslBuilder = message(BytesJmsMessage(bytes))
    def mapMessage(map: Map[String, Any]): SendDslBuilder = mapMessage(map.expressionSuccess)
    def mapMessage(map: Expression[Map[String, Any]]): SendDslBuilder = message(MapJmsMessage(map))
    def objectMessage(o: Expression[JSerializable]): SendDslBuilder = message(ObjectJmsMessage(o))

    private def message(mess: JmsMessage) = SendDslBuilder(JmsAttributes(requestName, destination, None, mess), new SendBuilder(_))
  }
}

final case class SendDslBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

  /**
   * Add JMS message properties (aka headers) to the outbound message
   */
  def property(key: Expression[String], value: Expression[Any]): SendDslBuilder = this.modify(_.attributes.messageProperties)(_ + (key -> value))

  def jmsType(jmsType: Expression[String]): SendDslBuilder = this.modify(_.attributes.jmsType).setTo(Some(jmsType))

  def build: ActionBuilder = factory(attributes)
}

object RequestReplyDslBuilder {
  final class Queue(requestName: Expression[String]) {
    def queue(name: Expression[String]): Message = destination(JmsDestination.Queue(name))
    def destination(destination: JmsDestination): Message =
      Message(requestName, destination, JmsDestination.TemporaryQueue, setJmsReplyTo = true, None, None)
  }

  final case class Message(
      requestName: Expression[String],
      destination: JmsDestination,
      replyDest: JmsDestination,
      setJmsReplyTo: Boolean,
      trackerDest: Option[JmsDestination],
      selector: Option[Expression[String]]
  ) {

    /**
     * Add a reply queue, if not specified dynamic queue is used
     */
    def replyQueue(name: Expression[String]): Message = replyDestination(JmsDestination.Queue(name))
    def replyDestination(destination: JmsDestination): Message = this.copy(replyDest = destination)
    def noJmsReplyTo: Message = this.copy(setJmsReplyTo = false)
    def trackerQueue(name: Expression[String]): Message = trackerDestination(JmsDestination.Queue(name))
    def trackerDestination(destination: JmsDestination): Message = this.copy(trackerDest = Some(destination))

    /**
     * defines selector for reply destination that is used for responses
     */
    def selector(select: Expression[String]): Message = this.copy(selector = Some(select))

    def textMessage(text: Expression[String]): RequestReplyDslBuilder = message(TextJmsMessage(text))
    def bytesMessage(bytes: Expression[Array[Byte]]): RequestReplyDslBuilder = message(BytesJmsMessage(bytes))
    def mapMessage(map: Map[String, Any]): RequestReplyDslBuilder = mapMessage(map.expressionSuccess)
    def mapMessage(map: Expression[Map[String, Any]]): RequestReplyDslBuilder = message(MapJmsMessage(map))
    def objectMessage(o: Expression[JSerializable]): RequestReplyDslBuilder = message(ObjectJmsMessage(o))

    private def message(mess: JmsMessage) =
      RequestReplyDslBuilder(
        JmsAttributes(requestName, destination, selector, mess),
        RequestReplyBuilder.apply(_, replyDest, setJmsReplyTo, trackerDest)
      )
  }
}

final case class RequestReplyDslBuilder(attributes: JmsAttributes, factory: JmsAttributes => ActionBuilder) {

  /**
   * Add JMS message properties (aka headers) to the outbound message
   */
  def property(key: Expression[String], value: Expression[Any]): RequestReplyDslBuilder = this.modify(_.attributes.messageProperties)(_ + (key -> value))

  def jmsType(jmsType: Expression[String]): RequestReplyDslBuilder = this.modify(_.attributes.jmsType).setTo(Some(jmsType))

  /**
   * Add a check that will be performed on each received JMS response message before giving Gatling on OK/KO response
   */
  def check(checks: JmsCheck*): RequestReplyDslBuilder = {
    require(!checks.contains(null), "Checks can't contain null elements. Forward reference issue?")
    this.modify(_.attributes.checks)(_ ::: checks.toList)
  }

  def checkIf(condition: Expression[Boolean])(thenChecks: JmsCheck*): RequestReplyDslBuilder =
    check(thenChecks.map(_.checkIf(condition)): _*)

  def checkIf(condition: (Message, Session) => Validation[Boolean])(thenChecks: JmsCheck*): RequestReplyDslBuilder =
    check(thenChecks.map(_.checkIf(condition)): _*)

  def build: ActionBuilder = factory(attributes)
}
