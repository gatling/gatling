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

import io.gatling.core.Predef._
import scala.concurrent.duration._
//#imports
import io.gatling.mqtt.Predef._
//#imports

class MqttProtocolSampleScala {
//#protocol-sample
val mqttProtocol = mqtt
  // enable protocol version 3.1 (default: false)
  .mqttVersion_3_1
  // enable protocol version 3.1.1 (default: true)
  .mqttVersion_3_1_1
  // broker address (default: localhost:1883)
  .broker("hostname", 1883)
  // if TLS should be enabled (default: false)
  .useTls(true)
  // Used to specify KeyManagerFactory for each individual virtual user. Input is the 0-based incremental id of the virtual user.
  .perUserKeyManagerFactory(userId => null.asInstanceOf[javax.net.ssl.KeyManagerFactory])
  // clientIdentifier sent in the connect payload (of not set, Gatling will generate a random one)
  .clientId("#{id}")
  // if session should be cleaned during connect (default: true)
  .cleanSession(true)
  // optional credentials for connecting
  .credentials("#{userName}", "#{password}")
  // connections keep alive timeout
  .keepAlive(30)
  // use at-most-once QoS (default: true)
  .qosAtMostOnce
  // use at-least-once QoS (default: false)
  .qosAtLeastOnce
  // use exactly-once QoS (default: false)
  .qosExactlyOnce
  // enable retain (default: false)
  .retain(false)
  // send last will, possibly with specific QoS and retain
  .lastWill(
    LastWill("#{willTopic}", StringBody("#{willMessage}"))
      .qosAtLeastOnce
      .retain(true)
  )
  // max number of reconnects after connection crash (default: 3)
  .reconnectAttemptsMax(1)
  // reconnect delay after connection crash in millis (default: 100)
  .reconnectDelay(1)
  // reconnect delay exponential backoff (default: 1.5)
  .reconnectBackoffMultiplier(1.5F)
  //  resend delay after send failure in millis (default: 5000)
  .resendDelay(1000)
  // resend delay exponential backoff (default: 1.0)
  .resendBackoffMultiplier(2.0F)
  // interval for timeout checker (default: 1 second)
  .timeoutCheckInterval(1)
  // check for pairing messages sent and messages received
  .correlateBy(null)
//#protocol-sample

//#connect
mqtt("Connecting").connect
//#connect

//#subscribe
mqtt("Subscribing")
  .subscribe("#{myTopic}")
  // optional, override default QoS
  .qosAtMostOnce
//#subscribe

//#publish
mqtt("Publishing")
  .publish("#{myTopic}")
  .message(StringBody("#{myTextPayload}"))
//#publish

//#check
// subscribe and expect to receive a message within 100ms, without blocking flow
mqtt("Subscribing").subscribe("#{myTopic2}")
  .expect(100.milliseconds)

// publish and await (block) until it receives a message withing 100ms
mqtt("Publishing").publish("#{myTopic}").message(StringBody("#{myPayload}"))
  .await(100.milliseconds)

// optionally, define in which topic the expected message will be received
mqtt("Publishing").publish("#{myTopic}").message(StringBody("#{myPayload}"))
  .await(100.milliseconds, "repub/#{myTopic}")

// optionally define check criteria to be applied on the matching received message
mqtt("Publishing")
  .publish("#{myTopic}").message(StringBody("#{myPayload}"))
  .await(100.milliseconds).check(jsonPath("$.error").notExists)
//#check

//#waitForMessages
waitForMessages.timeout(100.milliseconds)
//#waitForMessages

//#process
// store the unmatched messages in the Session
processUnmatchedMessages("#{myTopic}") {
  (messages, session) => session.set("messages", messages)
}

// collect the last text message and store it in the Session
processUnmatchedMessages("#{myTopic}") {
  (messages, session) => {
    val lastMessage = messages.reverseIterator.nextOption().map(_.payloadUtf8String)
    lastMessage.fold(session)(m => session.set("lastTextMessage", m))
  }
}
//#process

//#example
class MqttSample extends Simulation {
  val mqttProtocol = mqtt
    .broker("localhost", 1883)
    .correlateBy(jsonPath("$.correlationId"))

  val scn = scenario("MQTT Test")
    .feed(csv("topics-and-payloads.csv"))
    .exec(mqtt("Connecting").connect)
    .exec(mqtt("Subscribing").subscribe("#{myTopic}"))
    .exec(mqtt("Publishing").publish("#{myTopic}")
      .message(StringBody("#{myTextPayload}"))
      .expect(100.milliseconds).check(jsonPath("$.error").notExists))

  setUp(scn.inject(rampUsersPerSec(10) to 1000 during (60)))
    .protocols(mqttProtocol)
}
//#example
}
