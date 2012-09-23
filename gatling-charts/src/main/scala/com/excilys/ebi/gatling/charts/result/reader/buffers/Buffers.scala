/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.charts.result.reader.buffers

import java.util.{ HashMap => JHashMap }

trait Buffers {

	def getBuffer[B](key: BufferKey, buffers: JHashMap[BufferKey, B], builder: () => B): B = {
		if (buffers.containsKey(key))
			buffers.get(key)
		else {
			val buffer = builder()
			buffers.put(key, buffer)
			buffer
		}
	}
}

class CountBuffer {
	implicit val map = new JHashMap[Long, Long]

	def update(bucket: Long) { initOrUpdateJHashMapEntry(bucket, 1L, (value: Long) => value + 1) }
}

class RangeBuffer {
	implicit val map = new JHashMap[Long, (Long, Long)]

	def update(bucket: Long, value: Long) {
		initOrUpdateJHashMapEntry(bucket, (value, value), (minMax: (Long, Long)) => {
			val (minValue, maxValue) = minMax
			(value min minValue, value max maxValue)
		})
	}
}

