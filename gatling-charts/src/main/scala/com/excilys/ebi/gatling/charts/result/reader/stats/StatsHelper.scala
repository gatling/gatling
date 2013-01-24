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
package com.excilys.ebi.gatling.charts.result.reader.stats

object StatsHelper {
	def bucketsList(min: Int, max: Int, step: Double): List[Int] = {
		val halfStep = step / 2
		(0 until math.ceil((max - min) / step).toInt).map(i => math.round(min + step * i + halfStep).toInt).toList
	}

	def step(min: Int, max: Int, maxPlots: Int): Double = {
		val range = max - min
		if (range < maxPlots) 1.0
		else range / maxPlots.toDouble
	}

	def bucket(t: Int, min: Int, max: Int, step: Double, halfStep: Double): Int = {
		val value = t min (max - 1)
		math.round((value - (value - min) % step + halfStep)).toInt
	}

	def square(x: Double) = x * x

	def square(x: Int) = x * x

	def stdDev(squareMean: Double, mean: Double) = math.sqrt(squareMean - square(mean))
}
