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

import scala.util.Random.nextInt

object Sample {
	val SAMPLE_SIZE = 1028
}

class Sample {

	private var count = 0
	private val values: Array[Long] = Array.ofDim(Sample.SAMPLE_SIZE)

	def update(value: Long) {
		if (count < values.length) {
			values(count) = value
		} else {
			val random = nextInt(count + 1)
			if (random < values.length) {
				values(random) = value
			}
		}
		count += 1
	}

	def getQuantile(quantile: Int) = {
		val sortedValues = values.sorted
		val size = count.min(values.length)
		if (size == 0)
			0
		else
			(quantile / 100.) * (size + 1) match {
				case index if (index < 1) => sortedValues(0)
				case index if (index >= size) => sortedValues(size - 1)
				case index => sortedValues(index.toInt)
			}
	}
}
