/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.metrics.reporting

import java.io.{ BufferedWriter, IOException, OutputStreamWriter }
import java.net.Socket
import java.util.Locale
import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions.{ collectionAsScalaIterable, mapAsScalaMap }

import com.excilys.ebi.gatling.core.util.StringHelper
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.metrics.core.{ GatlingMetricsProcessor, GatlingMetricsRegistry }
import com.excilys.ebi.gatling.metrics.types.{ FastCounter, FastHistogram }
import com.yammer.metrics.core.{ MetricName, MetricPredicate, MetricsRegistry }
import com.yammer.metrics.reporting.AbstractPollingReporter

import grizzled.slf4j.Logging

object GatlingGraphiteReporter extends Logging {

	private var reporter: GatlingGraphiteReporter = null

	def enable(host: String, port: Int) {
		reporter = new GatlingGraphiteReporter(host, port)
		reporter.start(1, TimeUnit.SECONDS)
	}

	def shutdown {
		reporter.shutdown
	}
}
class GatlingGraphiteReporter(host: String, port: Int, registry: MetricsRegistry = GatlingMetricsRegistry.registry, name: String = "graphite-reporter")
		extends AbstractPollingReporter(registry, name) with GatlingMetricsProcessor[Long] with Logging {

	private val socket = new Socket(host, port)
	private val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))

	def run {
		try {
			val epoch: Long = nowMillis / 1000
			printMetrics(epoch)
			writer.flush
		} catch {
			case e: Exception => {
				error("Error writing to Graphite", e)
				if (writer != null) {
					try {
						writer.flush
					} catch {
						case e1: IOException => error("Error while flushing writer", e1)
					}
				}
			}
		}
	}

	override def shutdown {
		socket.close
		super.shutdown
	}

	private def printMetrics(epoch: Long) {

		getMetricsRegistry
			.groupedMetrics(MetricPredicate.ALL)
			.values.flatten
			.foreach { case (name, metric) => metric.processWith(this, name, epoch) }
	}

	private def sanitizeName(name: MetricName): String = {
		val sb: StringBuilder = new StringBuilder().append(".").append(name.getType).append(".")
		if (name.hasScope) {
			sb.append(name.getScope).append('.')
		}
		sb.append(name.getName).toString
	}

	private def sanitizeString(s: String): String = s.replace(' ', '-')

	private def sendToGraphite(timestamp: Long, name: String, value: String) {
		try {
			writer.write(sanitizeString(name))
			writer.write('.')
			writer.write(value)
			writer.write(' ')
			writer.write(timestamp.toString)
			writer.write(StringHelper.END_OF_LINE)
			writer.flush
		} catch {
			case e: IOException => error("Error sending to Graphite:", e)
		}
	}

	/**
	 * Process the given fast counter.
	 *
	 * @param name       the name of the counter
	 * @param counter    the counter
	 * @param epoch    the epoch of the meter
	 * @throws Exception if something goes wrong
	 */
	override def processFastCounter(name: MetricName, counter: FastCounter, epoch: Long) {
		sendInt(epoch, sanitizeName(name), "count", counter.count)
	}

	/**
	 * Process the given fast histogram.
	 *
	 * @param name       the name of the histogram
	 * @param histogram  the histogram
	 * @param epoch    the epoch of the meter
	 * @throws Exception if something goes wrong
	 */
	override def processFastHistogram(name: MetricName, histogram: FastHistogram, epoch: Long) {
		sendStats(epoch, sanitizeName(name), histogram)
	}

	private def sendInt(timestamp: Long, name: String, valueName: String, value: Long) {
		sendToGraphite(timestamp, name, valueName + " " + "%d".formatLocal(Locale.US, value))
	}

	private def sendFloat(timestamp: Long, name: String, valueName: String, value: Double) {
		sendToGraphite(timestamp, name, valueName + " " + "%2.2f".formatLocal(Locale.US, value))
	}

	private def sendStats(epoch: Long, sanitizedName: String, histogram: FastHistogram) {
		val stats = histogram.getStats
		sendFloat(epoch, sanitizedName, "min", stats.min)
		sendFloat(epoch, sanitizedName, "max", stats.max)
		sendFloat(epoch, sanitizedName, "mean", stats.mean)
		sendFloat(epoch, sanitizedName, "stddev", stats.stdDev)
		sendFloat(epoch, sanitizedName, "median", stats.snapshot.getMedian)
		sendFloat(epoch, sanitizedName, "75percentile", stats.snapshot.get75thPercentile)
		sendFloat(epoch, sanitizedName, "95percentile", stats.snapshot.get95thPercentile)
		sendFloat(epoch, sanitizedName, "98percentile", stats.snapshot.get98thPercentile)
		sendFloat(epoch, sanitizedName, "99percentile", stats.snapshot.get99thPercentile)
		sendFloat(epoch, sanitizedName, "999percentile", stats.snapshot.get999thPercentile)
	}
}
