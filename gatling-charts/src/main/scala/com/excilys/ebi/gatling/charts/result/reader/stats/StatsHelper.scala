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
package com.excilys.ebi.gatling.charts.result.reader.stats

import scala.collection.mutable

import com.excilys.ebi.gatling.charts.result.reader.scalding.GatlingBufferSource

import cascading.tuple.TupleEntry

object StatsHelper {
	def bucketsList(min: Long, max: Long, step: Double): List[Long] = {
		val demiStep = step / 2
		(0 until math.round((max - min) / step).toInt).map(i => math.round(min + step * i + demiStep)).toList
	}

	def step(min: Long, max: Long, maxPlots: Int) = {
		val range = max - min
		if (range < maxPlots) 1.0
		else range / maxPlots.toDouble
	}

	def bucket(t: Long, min: Long, max: Long, step: Double, demiStep: Double) = {
		if (t >= max) math.round((max - demiStep))
		else math.round((t - (t - min) % step + demiStep))
	}

	def output[A](buffer: mutable.Buffer[A])(implicit parseFunction: (TupleEntry) => A) = {
		new GatlingBufferSource(buffer, parseFunction)
	}

	def square(x: Double) = x * x

	def square(x: Long) = x * x

	def stdDev(squareMean: Double, mean: Double) = math.sqrt(squareMean - square(mean))
}
