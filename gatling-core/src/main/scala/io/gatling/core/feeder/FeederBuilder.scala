/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
	def build: Feeder[T]
}

case class FeederWrapper[T](build: Feeder[T]) extends FeederBuilder[T]

case class RecordArrayFeederBuilder[T](array: Array[Record[T]], strategy: FeederStrategy = Queue) extends FeederBuilder[T] {

	def convert(conversion: PartialFunction[(String, T), Any]): RecordArrayFeederBuilder[Any] = {
		val useValueAsIs: PartialFunction[(String, T), Any] = { case (_, value) => value }
		val fullConversion = conversion orElse useValueAsIs

		copy[Any](array = array.map(_.map { case (key, value) => key -> fullConversion(key, value) }))
	}

	def build: Feeder[T] = strategy match {
		case Queue => array.iterator
		case Random => new Feeder[T] {
			def hasNext = array.length != 0
			def next = array(ThreadLocalRandom.current.nextInt(array.length))
		}
		case Circular => RoundRobin(array)
	}

	def queue = copy(strategy = Queue)
	def random = copy(strategy = Random)
	def circular = copy(strategy = Circular)
}
