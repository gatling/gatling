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

package io.gatling.jms.protocol

import java.util.UUID
import javax.jms.Message

import com.eatthepath.uuid.FastUUID

/**
 * define trait for message matching logic with separate request/response
 * to see how it can be used check JmsDefaultMessageMatcher
 */
trait JmsMessageMatcher {
  def prepareRequest(msg: Message): Unit
  def requestMatchId(msg: Message): String
  def responseMatchId(msg: Message): String
}

object MessageIdMessageMatcher extends JmsMessageMatcher {
  override def prepareRequest(msg: Message): Unit = {}
  override def requestMatchId(msg: Message): String = msg.getJMSMessageID
  override def responseMatchId(msg: Message): String = msg.getJMSCorrelationID
}

object CorrelationIdMessageMatcher extends JmsMessageMatcher {
  override def prepareRequest(msg: Message): Unit = msg.setJMSCorrelationID(FastUUID.toString(UUID.randomUUID))
  override def requestMatchId(msg: Message): String = msg.getJMSCorrelationID
  override def responseMatchId(msg: Message): String = msg.getJMSCorrelationID
}
