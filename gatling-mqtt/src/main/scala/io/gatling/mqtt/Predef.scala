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

package io.gatling.mqtt

import io.gatling.core.body._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.mqtt.action.builder._
import io.gatling.mqtt.check.MqttCheckSupport
import io.gatling.mqtt.protocol.{ LastWillBuilder, MqttProtocolBuilder }

import com.typesafe.scalalogging.StrictLogging

object Predef extends MqttCheckSupport with StrictLogging {

  def LastWill(topic: Expression[String], message: Body): LastWillBuilder = ???

  // NB We need this implicit parameter even though it's not used so compiler can tell appart this method and io.gatling.mqtt(requestName)
  def mqtt(implicit configuration: GatlingConfiguration): MqttProtocolBuilder = ???

  def mqtt(requestName: Expression[String]): MqttActionBuilderBase = ???

  def waitForMessages: WaitForMessagesBuilder = WaitForMessagesBuilder.Default
}
