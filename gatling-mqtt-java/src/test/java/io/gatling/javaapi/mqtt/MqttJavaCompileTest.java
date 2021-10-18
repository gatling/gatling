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

package io.gatling.javaapi.mqtt;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.mqtt.MqttDsl.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;

public class MqttJavaCompileTest extends Simulation {

  private MqttProtocolBuilder mqttProtocol =
      mqtt.broker("localhost", 1883)
          .useTls(false)
          .perUserKeyManagerFactory(
              session -> {
                try {
                  return KeyManagerFactory.getInstance("TLS");
                } catch (NoSuchAlgorithmException e) {
                  throw new RuntimeException(e);
                }
              })
          .clientId("${clientId}")
          .cleanSession(true)
          .credentials("${userName}", "${password}")
          .keepAlive(30)
          // default QoS
          .qosAtMostOnce()
          .qosAtLeastOnce()
          .qosExactlyOnce()
          // default retain
          .retain(true)
          .lastWill(
              LastWill("${willTopic}", StringBody("${willMessage}")).qosAtLeastOnce().retain(true))
          .lastWill(LastWill("${willTopic}", ByteArrayBody(new byte[] {0})))
          .lastWill(LastWill("${willTopic}", RawFileBody("file")))
          .mqttVersion_3_1()
          .mqttVersion_3_1_1()
          .reconnectAttemptsMax(1)
          .reconnectDelay(1)
          .reconnectBackoffMultiplier(1.2f)
          .resendDelay(1)
          .resendBackoffMultiplier(2.0f)
          .timeoutCheckInterval(Duration.ofSeconds(1))
          .correlateBy(jsonPath("$.correlationId"))
          .correlateBy(jsonPath("$.correlationId").find())
          .correlateBy(regex("{correlationId=(.*)}.*"))
          .correlateBy(regex("{correlationId=(.*)}.*").find())
          .correlateBy(bodyString().transform(text -> text.substring(3)))
          .correlateBy(bodyBytes().transform(bytes -> new String(bytes, 0, 3, UTF_8)));

  private ScenarioBuilder scn =
      scenario("MQTT Test")
          .exec(mqtt("Connecting").connect())
          .exec(
              mqtt("Subscribing")
                  .subscribe("${myTopic}")
                  .qosAtMostOnce()
                  .qosAtLeastOnce()
                  .qosExactlyOnce())
          .exec(mqtt("Subscribing").subscribe("${myTopic2}").wait(Duration.ofMillis(100)))
          .exec(
              mqtt("Subscribing")
                  .subscribe("${myTopic2}")
                  .wait(1)
                  .check(jsonPath("$.error").notExists()))
          .exec(mqtt("Subscribing").subscribe("${myTopic2}").expect(Duration.ofMillis(100)))
          .exec(
              mqtt("Subscribing")
                  .subscribe("${myTopic2}")
                  .expect(1)
                  .check(jsonPath("$.error").notExists()))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myTextPayload}"))
                  .qosAtMostOnce()
                  .qosAtLeastOnce()
                  .qosExactlyOnce()
              //        .retain(true)
              )
          .exec(mqtt("Publishing").publish("${myTopic}").message(ElFileBody("file")))
          .exec(mqtt("Publishing").publish("${myTopic}").message(PebbleFileBody("file")))
          .exec(
              mqtt("Publishing").publish("${myTopic}").message(ByteArrayBody("${myBinaryPayload}")))
          .exec(mqtt("Publishing").publish("${myTopic}").message(RawFileBody("file")))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myPayload}"))
                  .wait(Duration.ofMillis(100)))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myPayload}"))
                  .wait(Duration.ofMillis(100))
                  .check(jsonPath("$.error").notExists()))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myPayload}"))
                  .expect(Duration.ofMillis(100)))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myPayload}"))
                  .expect(Duration.ofMillis(100), "repub/${myTopic}"))
          .exec(
              mqtt("Publishing")
                  .publish("${myTopic}")
                  .message(StringBody("${myPayload}"))
                  .expect(Duration.ofMillis(100))
                  .check(jsonPath("$.error").notExists()))
          .exec(waitForMessages().timeout(Duration.ofMillis(100)));

  public MqttJavaCompileTest() {
    setUp(scn.injectOpen(atOnceUsers(1))).protocols(mqttProtocol);
  }
}
