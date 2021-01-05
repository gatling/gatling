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

import java.net.InetSocketAddress

import scala.concurrent.duration._

import io.gatling.commons.model.Credentials
import io.gatling.core.protocol.Protocol
import io.gatling.core.session.Expression
import io.gatling.mqtt.check.MessageCorrelator

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.mqtt.{ MqttQoS, MqttVersion }

object MqttProtocol extends StrictLogging {

  private val DefaultBrokerAddress = new InetSocketAddress("localhost", 1883)

  val Default: MqttProtocol =
    new MqttProtocol(
      version = MqttVersion.MQTT_3_1_1,
      brokerAddress = DefaultBrokerAddress,
      useTls = false,
      clientId = None,
      cleanSession = true,
      credentials = None,
      lastWill = None,
      connectTimeout = 5.seconds,
      keepAlive = 5.seconds,
      qos = MqttQoS.AT_MOST_ONCE,
      retain = false,
      reconnect = MqttProtocolReconnectPart(
        reconnectAttemptsMax = 3,
        reconnectDelay = 100,
        reconnectBackOffMultiplier = 1.5f
      ),
      resend = MqttProtocolResendPart(
        resendDelay = 5000,
        resendBackoffMultiplier = 1f
      ),
      correlator = None,
      timeoutCheckInterval = 1.second
    )
}

final case class MqttProtocol(
    version: MqttVersion,
    brokerAddress: InetSocketAddress,
    useTls: Boolean,
    clientId: Option[Expression[String]],
    cleanSession: Boolean,
    credentials: Option[Expression[Credentials]],
    connectTimeout: FiniteDuration,
    keepAlive: FiniteDuration,
    qos: MqttQoS,
    retain: Boolean,
    reconnect: MqttProtocolReconnectPart,
    resend: MqttProtocolResendPart,
    lastWill: Option[LastWill],
    correlator: Option[MessageCorrelator],
    timeoutCheckInterval: FiniteDuration
) extends Protocol

final case class MqttProtocolReconnectPart(
    reconnectAttemptsMax: Int,
    reconnectDelay: Long,
    reconnectBackOffMultiplier: Float
)

final case class MqttProtocolResendPart(
    resendDelay: Long,
    resendBackoffMultiplier: Float
)
