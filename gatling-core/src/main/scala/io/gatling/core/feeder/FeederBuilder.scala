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
package io.gatling.core.feeder

import scala.concurrent.forkjoin.ThreadLocalRandom

import io.gatling.core.util.RoundRobin

trait FeederBuilder[T] {
	private[gatling] def build: Feeder[T]
}

case class FeederWrapper[T](build: Feeder[T]) extends FeederBuilder[T]

case class AdvancedFeederBuilder[T](data: Array[Record[T]], strategy: Strategy = Queue) extends FeederBuilder[T] {

	def convert(conversions: (String, T => Any)*): AdvancedFeederBuilder[Any] = {
		val indexedConversions = conversions.toMap.withDefaultValue(identity[T] _)
		copy(data = data.map(_.map { case (key, value) => (key, indexedConversions(key)(value)) }))
	}

	private[gatling] def build: Feeder[T] = strategy match {
		case Queue => data.iterator
		case Random => new Feeder[T] {
			def hasNext = data.length != 0
			def next = data(ThreadLocalRandom.current.nextInt(data.length))
		}
		case Circular => RoundRobin(data)
	}

	def queue: AdvancedFeederBuilder[T] = copy(strategy = Queue)
	def random: AdvancedFeederBuilder[T] = copy(strategy = Random)
	def circular: AdvancedFeederBuilder[T] = copy(strategy = Circular)
}