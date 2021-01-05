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

package io.gatling.mqtt.protocol

import io.gatling.core.session.Expression

import io.netty.handler.codec.mqtt.MqttQoS

object LastWillBuilder {
  def apply(topic: Expression[String], message: Expression[Array[Byte]]): LastWillBuilder =
    new LastWillBuilder(topic, message, qosOverride = None, retainOverride = None)
}

final case class LastWillBuilder(
    topic: Expression[String],
    message: Expression[Array[Byte]],
    qosOverride: Option[MqttQoS],
    retainOverride: Option[Boolean]
) {
  def qosAtMostOnce: LastWillBuilder = qos(MqttQoS.AT_MOST_ONCE)
  def qosAtLeastOnce: LastWillBuilder = qos(MqttQoS.AT_LEAST_ONCE)
  def qosExactlyOnce: LastWillBuilder = qos(MqttQoS.EXACTLY_ONCE)
  private def qos(newQos: MqttQoS): LastWillBuilder = copy(qosOverride = Some(newQos))
  def retain(newRetain: Boolean): LastWillBuilder = copy(retainOverride = Some(newRetain))

  def build: LastWill = LastWill(topic, message, qosOverride, retainOverride)
}

final case class LastWill(topic: Expression[String], message: Expression[Array[Byte]], qosOverride: Option[MqttQoS], retainOverride: Option[Boolean])
