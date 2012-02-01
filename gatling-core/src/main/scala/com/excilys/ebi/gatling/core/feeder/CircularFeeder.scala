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
package com.excilys.ebi.gatling.core.feeder

import java.util.concurrent.atomic.AtomicInteger

import com.twitter.util.RingBuffer

class CircularFeeder(feederSource: FeederSource) extends Feeder {

	private val bufferSize = feederSource.values.size
	private val currentIndex = new AtomicInteger(0)

	private val values = new RingBuffer[Map[String, String]](bufferSize)

	values ++= feederSource.values

	def next: Map[String, String] = values(currentIndex.getAndAdd(1) % bufferSize)
}