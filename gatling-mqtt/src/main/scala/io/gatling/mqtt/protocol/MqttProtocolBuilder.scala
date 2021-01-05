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

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.model.Credentials
import io.gatling.core.session.Expression
import io.gatling.mqtt.check.MessageCorrelator

import com.softwaremill.quicklens._
import io.netty.handler.codec.mqtt.{ MqttQoS, MqttVersion }

final case class MqttProtocolBuilder(mqttProtocol: MqttProtocol) {

  def mqttVersion_3_1: MqttProtocolBuilder =
    this.modify(_.mqttProtocol.version).setTo(MqttVersion.MQTT_3_1)

  def mqttVersion_3_1_1: MqttProtocolBuilder =
    this.modify(_.mqttProtocol.version).setTo(MqttVersion.MQTT_3_1_1)

  def broker(hostname: String, port: Int): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.brokerAddress).setTo(new InetSocketAddress(hostname, port))

  def useTls(useTls: Boolean): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.useTls).setTo(useTls)

  def clientId(clientId: Expression[String]): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.clientId).setTo(Some(clientId))

  def cleanSession(cleanSession: Boolean): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.cleanSession).setTo(cleanSession)

  def connectTimeout(connectTimeout: FiniteDuration): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.connectTimeout).setTo(connectTimeout)

  def keepAlive(keepAlive: FiniteDuration): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.keepAlive).setTo(keepAlive)

  def qosAtMostOnce: MqttProtocolBuilder = qos(MqttQoS.AT_MOST_ONCE)
  def qosAtLeastOnce: MqttProtocolBuilder = qos(MqttQoS.AT_LEAST_ONCE)
  def qosExactlyOnce: MqttProtocolBuilder = qos(MqttQoS.EXACTLY_ONCE)
  private def qos(newQos: MqttQoS): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.qos).setTo(newQos)

  def retain(newRetain: Boolean): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.retain).setTo(newRetain)

  def credentials(userName: Expression[String], password: Expression[String]): MqttProtocolBuilder = {
    val credentials: Expression[Credentials] = session =>
      for {
        un <- userName(session)
        pwd <- password(session)
      } yield Credentials(un, pwd)

    this.modify(_.mqttProtocol.credentials).setTo(Some(credentials))
  }

  def lastWill(lw: LastWillBuilder): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.lastWill).setTo(Some(lw.build))

  def reconnectAttemptsMax(reconnectAttemptsMax: Int): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.reconnect.reconnectAttemptsMax).setTo(reconnectAttemptsMax)

  def reconnectDelay(reconnectDelay: Long): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.reconnect.reconnectDelay).setTo(reconnectDelay)

  def reconnectBackoffMultiplier(reconnectBackOffMultiplier: Float): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.reconnect.reconnectBackOffMultiplier).setTo(reconnectBackOffMultiplier)

  def resendDelay(delay: Int): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.resend.resendDelay).setTo(delay)

  def resendBackoffMultiplier(multiplier: Float): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.resend.resendBackoffMultiplier).setTo(multiplier)

  def correlateBy(correlator: MessageCorrelator): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.correlator).setTo(Some(correlator))

  def timeoutCheckInterval(interval: FiniteDuration): MqttProtocolBuilder =
    this.modify(_.mqttProtocol.timeoutCheckInterval).setTo(interval)

  def build: MqttProtocol = mqttProtocol
}

object MqttProtocolBuilder {

  implicit def toMqttProtocol(builder: MqttProtocolBuilder): MqttProtocol = builder.build

  val Default: MqttProtocolBuilder = MqttProtocolBuilder(MqttProtocol.Default)
}
