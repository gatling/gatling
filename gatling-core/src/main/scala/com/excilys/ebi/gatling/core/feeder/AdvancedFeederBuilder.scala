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
package com.excilys.ebi.gatling.core.feeder

import scala.concurrent.forkjoin.ThreadLocalRandom

import com.excilys.ebi.gatling.core.util.RoundRobin

trait AdvancedFeederBuilder[T] extends FeederBuilder[T] {

	private[core] def data: Array[Map[String, T]]
	private[core] def strategy: Strategy = Queue

	def convert(conversions: (String, T => Any)*): AdvancedFeederBuilder[Any] = new FeederBuilderConverter(this, conversions)

	private[gatling] def build: Feeder[T] = strategy match {
		case Queue => data.iterator
		case Random => new Feeder[T] {
			def hasNext = !data.isEmpty
			def next = data(ThreadLocalRandom.current.nextInt(data.size))
		}
		case Circular => RoundRobin(data)
	}

	def queue: AdvancedFeederBuilder[T] = FeederBuilderWithStrategy(this, Queue)
	def random: AdvancedFeederBuilder[T] = FeederBuilderWithStrategy(this, Random)
	def circular: AdvancedFeederBuilder[T] = FeederBuilderWithStrategy(this, Circular)
}

case class FeederBuilderFromArray[T](data: Array[Map[String, T]]) extends AdvancedFeederBuilder[T]

case class FeederBuilderWithStrategy[T](feederBuilder: AdvancedFeederBuilder[T], override val strategy: Strategy) extends AdvancedFeederBuilder[T] {
	def data = feederBuilder.data
}
