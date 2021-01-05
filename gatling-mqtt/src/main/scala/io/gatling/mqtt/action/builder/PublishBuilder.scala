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
import io.gatling.core.body._
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.mqtt.check.{ MqttCheck, MqttExpectation }

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.mqtt.MqttQoS

trait CheckablePublishBuilder { this: PublishBuilder =>
  def check(ck: MqttCheck): PublishBuilder =
    PublishBuilder(requestName, topic, body, qosOverride, retainOverride, expectation.map(_.copy(check = Some(ck))))
}

@SuppressWarnings(Array("org.wartremover.warts.FinalCaseClass"))
// because PublishBuilder with CheckablePublishBuilder
case class PublishBuilder(
    requestName: Expression[String],
    topic: Expression[String],
    body: Body,
    qosOverride: Option[MqttQoS],
    retainOverride: Option[Boolean],
    expectation: Option[MqttExpectation]
) extends MqttActionBuilder
    with StrictLogging {

  def qosAtMostOnce: PublishBuilder = qos(MqttQoS.AT_MOST_ONCE)
  def qosAtLeastOnce: PublishBuilder = qos(MqttQoS.AT_LEAST_ONCE)
  def qosExactlyOnce: PublishBuilder = qos(MqttQoS.EXACTLY_ONCE)
  private def qos(newQos: MqttQoS): PublishBuilder = copy(qosOverride = Some(newQos))

  def retain(newRetain: Boolean): PublishBuilder = copy(retainOverride = Some(newRetain))

  def wait(timeout: FiniteDuration): PublishBuilder with CheckablePublishBuilder =
    wait(timeout, null)
  def wait(timeout: FiniteDuration, expectedTopic: Expression[String]): PublishBuilder with CheckablePublishBuilder =
    new PublishBuilder(
      requestName,
      topic,
      body,
      qosOverride,
      retainOverride,
      Some(MqttExpectation(None, timeout, topic = Option(expectedTopic), blocking = true))
    ) with CheckablePublishBuilder

  def expect(timeout: FiniteDuration): PublishBuilder with CheckablePublishBuilder =
    expect(timeout, null)
  def expect(timeout: FiniteDuration, expectedTopic: Expression[String]): PublishBuilder with CheckablePublishBuilder =
    new PublishBuilder(
      requestName,
      topic,
      body,
      qosOverride,
      retainOverride,
      Some(MqttExpectation(None, timeout, topic = Option(expectedTopic), blocking = false))
    ) with CheckablePublishBuilder

  override def build(ctx: ScenarioContext, next: Action): Action = ???
}
