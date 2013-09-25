/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.metrics.sender

import java.net.{ DatagramPacket, DatagramSocket, InetSocketAddress }
import java.nio.channels.DatagramChannel

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.GatlingConfiguration.configuration

class UdpSender extends MetricsSender with AkkaDefaults {

	private val address = new InetSocketAddress(configuration.data.graphite.host, configuration.data.graphite.port)
	private val socket: DatagramSocket = DatagramChannel.open.socket
	system.registerOnTermination(socket.close)

	def sendToGraphite(bytes: Array[Byte]) {
		val packet = new DatagramPacket(bytes, bytes.length, address)
		socket.send(packet)
	}

	def flush {}
}
