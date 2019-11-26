/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.mqtt.config

import io.netty.channel.WriteBufferWaterMark
import javax.net.ssl.{ KeyManagerFactory, TrustManagerFactory }

final case class MqttConfiguration(
    useNativeTransport: Boolean,
    threadNumber: Option[Int],
    writeBufferWaterMark: WriteBufferWaterMark,
    socket: MqttSocketConfiguration,
    ssl: MqttSslConfiguration
)

final case class MqttSocketConfiguration(
    soReceiveBufferSize: Option[Int],
    soSendBufferSize: Option[Int],
    soLinger: Option[Int],
    tcpNoDelay: Boolean,
    ipTrafficClass: Option[Int]
)

final case class MqttSslConfiguration(
    useOpenSsl: Boolean,
    sslSessionCacheSize: Int,
    sslSessionTimeout: Int,
    useInsecureTrustManager: Boolean,
    keyManagerFactory: Option[KeyManagerFactory],
    trustManagerFactory: Option[TrustManagerFactory]
)
