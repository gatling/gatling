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

import scala.collection.JavaConversions.mapAsScalaMap
import com.yammer.metrics.core._
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.metrics.core.{GatlingMetrics, GatlingMetricsProcessor}
import com.excilys.ebi.gatling.metrics.types.{FastHistogram, FastCounter}
import java.io.{OutputStreamWriter, BufferedWriter, IOException}
import com.yammer.metrics.reporting.AbstractPollingReporter
import java.util.Locale
import com.yammer.metrics.stats.Snapshot
import java.util.concurrent.TimeUnit
import java.net.Socket
import com.excilys.ebi.gatling.core.util.StringHelper

object GatlingGraphiteReporter extends Logging {

	private var reporter : GatlingGraphiteReporter = null

	def enable(period: Long, unit: TimeUnit, host: String, port: Int) {
		reporter = new GatlingGraphiteReporter(host,port)
		reporter.start(period, unit)
	}

	def shutdown {
		reporter.shutdown
	}
}
class GatlingGraphiteReporter(host: String,port : Int,registry: MetricsRegistry = GatlingMetrics.registry,name: String = "graphite-reporter")
	extends AbstractPollingReporter(registry,name) with GatlingMetricsProcessor[Long] with Logging {

	private val metricsDispatcher = new GatlingMetricDispatcher
	private val socket = new Socket(host,port)
	private val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))

	def run {
		try {
			val epoch: Long = System.currentTimeMillis() / 1000
			printMetrics(epoch)
			writer.flush
		}
		catch {
			case e: Exception => {
				debug("Error writing to Graphite: {}", e)
				if (writer != null) {
					try {
						writer.flush
					} catch {
						case e1 : IOException => error("Error while flushing writer: {}",e1)
					}
				}
			}
		}
	}

	override def shutdown {
		socket.close()
		super.shutdown()
	}

	private def printMetrics(epoch: Long) {
		getMetricsRegistry.getGroupedMetrics(MetricPredicate.ALL).foreach(_._2.foreach(t => metricsDispatcher.dispatch(t._2, t._1, this, epoch)))
	}

	private def sanitizeName(name: MetricName): String = {
		val sb: StringBuilder = new StringBuilder().append(name.getDomain).append('.').append(name.getType).append('.')
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
		}
		catch {
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
		val sanitizedName = sanitizeName(name)
		sendSummarizable(epoch, sanitizedName, histogram)
		sendSampling(epoch, sanitizedName, histogram)
	}

	private def sendInt(timestamp: Long, name: String, valueName: String, value: Long) {
		sendToGraphite(timestamp, name, valueName + " " +"%d".formatLocal(Locale.US,value))
	}

	private def sendFloat(timestamp: Long, name: String, valueName: String, value: Double) {
		sendToGraphite(timestamp, name, valueName + " " + "%2.2f".formatLocal(Locale.US,value))
	}

	private def sendSummarizable(epoch: Long, sanitizedName: String, metric: Summarizable) {
		sendFloat(epoch, sanitizedName, "min", metric.getMin)
		sendFloat(epoch, sanitizedName, "max", metric.getMax)
		sendFloat(epoch, sanitizedName, "mean", metric.getMean)
		sendFloat(epoch, sanitizedName, "stddev", metric.getStdDev)
	}

	private def sendSampling(epoch: Long, sanitizedName: String, metric: Sampling) {
		val snapshot: Snapshot = metric.getSnapshot
		sendFloat(epoch, sanitizedName, "median", snapshot.getMedian)
		sendFloat(epoch, sanitizedName, "75percentile", snapshot.get75thPercentile)
		sendFloat(epoch, sanitizedName, "95percentile", snapshot.get95thPercentile)
		sendFloat(epoch, sanitizedName, "98percentile", snapshot.get98thPercentile)
		sendFloat(epoch, sanitizedName, "99percentile", snapshot.get99thPercentile)
		sendFloat(epoch, sanitizedName, "999percentile", snapshot.get999thPercentile)
	}
}
