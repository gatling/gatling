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

import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConversions.seqAsJavaList

import com.excilys.ebi.gatling.core.util.RoundRobin

class FeederBuiltIns(data: Array[Map[String, String]]) {

	def queue: Feeder = data.iterator

	def concurrentQueue = new ConcurrentLinkedQueue(data.toList).iterator

	def random: Feeder = {

		val random = new Random

		new Feeder {
			def hasNext = !data.isEmpty
			def next = data(random.nextInt(data.size))
		}
	}

	def circular: Feeder = RoundRobin(data)
}