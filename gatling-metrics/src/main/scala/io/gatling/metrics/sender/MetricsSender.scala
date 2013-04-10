/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import io.gatling.core.config.GatlingConfiguration.configuration

object MetricsSender {
	def newMetricsSender: MetricsSender = configuration.graphite.protocol match {
		case "tcp" => new TcpSender
		case "udp" => new UdpSender
	}
}
abstract class MetricsSender {

	def sendToGraphite(metricPath: String, value: Long, epoch: Long) {
		val bytes = raw"$metricPath $value $epoch".getBytes(configuration.core.encoding)
		sendToGraphite(bytes)

	}

	def sendToGraphite(bytes: Array[Byte])

	def flush

	def close
}
