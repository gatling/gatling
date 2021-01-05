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

package io.gatling.mqtt.action.builder

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.Action
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.mqtt.check.{ MqttCheck, MqttExpectation }

import io.netty.handler.codec.mqtt.MqttQoS

trait CheckableSubscribeBuilder { this: SubscribeBuilder =>
  def check(ck: MqttCheck): SubscribeBuilder =
    SubscribeBuilder(requestName, topic, qosOverride, expectation.map(_.copy(check = Some(ck))))
}

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
// because SubscribeBuilder with CheckableSubscribeBuilder
case class SubscribeBuilder(
    requestName: Expression[String],
    topic: Expression[String],
    qosOverride: Option[MqttQoS],
    expectation: Option[MqttExpectation]
) extends MqttActionBuilder {

  def qosAtMostOnce: SubscribeBuilder = qos(MqttQoS.AT_MOST_ONCE)
  def qosAtLeastOnce: SubscribeBuilder = qos(MqttQoS.AT_LEAST_ONCE)
  def qosExactlyOnce: SubscribeBuilder = qos(MqttQoS.EXACTLY_ONCE)
  private def qos(newQos: MqttQoS): SubscribeBuilder = copy(qosOverride = Some(newQos))

  def wait(timeout: FiniteDuration): SubscribeBuilder with CheckableSubscribeBuilder =
    wait(timeout, null)
  def wait(timeout: FiniteDuration, expectedTopic: Expression[String]): SubscribeBuilder with CheckableSubscribeBuilder =
    new SubscribeBuilder(requestName, topic, qosOverride, Some(MqttExpectation(None, timeout, topic = Option(expectedTopic), blocking = true)))
      with CheckableSubscribeBuilder

  def expect(timeout: FiniteDuration): SubscribeBuilder with CheckableSubscribeBuilder =
    expect(timeout, null)
  def expect(timeout: FiniteDuration, expectedTopic: Expression[String]): SubscribeBuilder with CheckableSubscribeBuilder =
    new SubscribeBuilder(requestName, topic, qosOverride, Some(MqttExpectation(None, timeout, topic = Option(expectedTopic), blocking = false)))
      with CheckableSubscribeBuilder

  override def build(ctx: ScenarioContext, next: Action): Action = ???
}
