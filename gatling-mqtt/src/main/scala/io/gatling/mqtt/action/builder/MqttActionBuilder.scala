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

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.body.Body
import io.gatling.core.session.Expression

class MqttActionBuilderBase(requestName: Expression[String]) {

  def connect: ConnectBuilder = new ConnectBuilder(requestName)

  def subscribe(topic: Expression[String]): SubscribeBuilder = SubscribeBuilder(requestName, topic, None, None)

  def publish(topic: Expression[String]): MqttActionPublishBase = new MqttActionPublishBase(requestName, topic)
}

class MqttActionPublishBase(requestName: Expression[String], topic: Expression[String]) {

  def message(body: Body): PublishBuilder = PublishBuilder(requestName, topic, body, None, None, None)
}

abstract class MqttActionBuilder extends ActionBuilder {}
