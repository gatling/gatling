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

import java.nio.charset.StandardCharsets.UTF_8

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.mqtt.Predef._

class MqttCompileTest extends Simulation {

  private val mqttConf = mqtt
    .broker("localhost", 1883)
    .useTls(false)
    .clientId("${clientId}")
    .cleanSession(true)
    .credentials("${userName}", "${password}")
    .keepAlive(30)
    // default QoS
    .qosAtMostOnce
    .qosAtLeastOnce
    .qosExactlyOnce
    // default retain
    .retain(true)
    .lastWill(LastWill("${willTopic}", StringBody("${willMessage}")).qosAtLeastOnce.retain(true))
    .lastWill(LastWill("${willTopic}", ByteArrayBody(new Array[Byte](0))))
    .lastWill(LastWill("${willTopic}", RawFileBody("file")))
    .mqttVersion_3_1
    .mqttVersion_3_1_1
    .reconnectAttemptsMax(1)
    .reconnectDelay(1L)
    .reconnectBackoffMultiplier(1.2f)
    .resendDelay(1000)
    .resendBackoffMultiplier(2.0f)
    .timeoutCheckInterval(1.second)
    .correlateBy(jsonPath("$.correlationId"))
    .correlateBy(jsonPath("$.correlationId").find)
    .correlateBy(regex("{correlationId=(.*)}.*"))
    .correlateBy(regex("{correlationId=(.*)}.*").find)
    .correlateBy(bodyString.transform(text => text.substring(3)))
    .correlateBy(bodyBytes.transform(bytes => new String(bytes.take(3), UTF_8)))

  private val scn = scenario("MQTT Test")
    .exec(mqtt("Connecting").connect)
    .exec(mqtt("Subscribing").subscribe("${myTopic}").qosAtMostOnce.qosAtLeastOnce.qosExactlyOnce)
    .exec(
      mqtt("Subscribing")
        .subscribe("${myTopic2}")
        .wait(100.milliseconds)
    )
    .exec(
      mqtt("Subscribing")
        .subscribe("${myTopic2}")
        .expect(100.milliseconds)
    )
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myTextPayload}"))
        .qosAtMostOnce
        .qosAtLeastOnce
        .qosExactlyOnce
        .retain(true)
    )
    .exec(mqtt("Publishing").publish("${myTopic}").message(ElFileBody("file")))
    .exec(mqtt("Publishing").publish("${myTopic}").message(PebbleFileBody("file")))
    .exec(mqtt("Publishing").publish("${myTopic}").message(ByteArrayBody("${myBinaryPayload}")))
    .exec(mqtt("Publishing").publish("${myTopic}").message(RawFileBody("file")))
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myPayload}"))
        .wait(100.milliseconds)
    )
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myPayload}"))
        .wait(100.milliseconds)
        .check(jsonPath("$.error").notExists)
    )
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myPayload}"))
        .expect(100.milliseconds)
    )
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myPayload}"))
        .expect(100.milliseconds, "repub/${myTopic}")
    )
    .exec(
      mqtt("Publishing")
        .publish("${myTopic}")
        .message(StringBody("${myPayload}"))
        .expect(100.milliseconds)
        .check(jsonPath("$.error").notExists)
    )
    .exec(waitForMessages.timeout(100.milliseconds))

  setUp(scn.inject(atOnceUsers(1))).protocols(mqttConf)
}
