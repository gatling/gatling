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
package com.excilys.ebi.gatling.log.stats

import com.excilys.ebi.gatling.log.scalding.GatlingBufferSource
import com.excilys.ebi.gatling.log.util.{ResultBufferType, ResultBufferFinderAndParser}

object StatsHelper {
	def bucketsList(min: Long, max: Long, step: Double) = {
		val demiStep = step / 2
		(0 until math.round((max - min) / step).toInt).map(i => math.round(min + step * i + demiStep))
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

	def output[A](resultBuffer: ResultBufferFinderAndParser[A], bufferType: ResultBufferType.ResultBufferType) = {
		new GatlingBufferSource(resultBuffer.bufferFinder(bufferType), resultBuffer.parseFunction)
	}

	def square(x: Double) = x * x

	def square(x: Long) = x * x
}
