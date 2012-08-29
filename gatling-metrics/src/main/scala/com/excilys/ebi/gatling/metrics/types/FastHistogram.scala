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
package com.excilys.ebi.gatling.metrics.types

import scala.math.sqrt

import com.excilys.ebi.gatling.metrics.core.GatlingMetricsProcessor
import com.yammer.metrics.core.{ Metric, MetricName, MetricProcessor }
import com.yammer.metrics.stats.{ Sample, Snapshot }

class FastHistogram(private val sample: Sample) extends Metric {

	private var min = 0L
	private var max = 0L
	private var sum = 0L
	private var count = 0L
	private val variance: Array[Double] = Array(-1.0, .0)

	def +=(value: Long) {
		count += 1
		max = value max max
		min = value min min
		sum += value
		sample.update(value)
		updateVariance(value)
	}

	def clear() {
		sample.clear
		count = 0
		max = Long.MinValue
		min = Long.MaxValue
		sum = 0
		variance(0) = -1.0
		variance(1) = .0
	}

	def getSnapshot = sample.getSnapshot

	def getStats = {
		val mean = if (count > 0L) sum / count.toDouble else .0
		val stdDev = if (count > 0L) sqrt(getVariance) else .0
		Stats(0L max max, 0L max min, mean, stdDev, sum, sample.getSnapshot)
	}

	private def getVariance = if (count > 0L) variance(1) / (count - 1) else .0

	private def updateVariance(value: Double) {
		val oldM = variance(0)
		variance(0) += (value - oldM) / count
		variance(1) += (value - oldM) * (value - variance(0))
	}

	def processWith[T](processor: MetricProcessor[T], name: MetricName, context: T) {
		processor match {
			case gatlingProcessor: GatlingMetricsProcessor[T] => gatlingProcessor.processFastHistogram(name, this, context)
			case _ =>
		}
	}

	case class Stats(max: Double, min: Double, mean: Double, stdDev: Double, sum: Double, snapshot: Snapshot)
}
