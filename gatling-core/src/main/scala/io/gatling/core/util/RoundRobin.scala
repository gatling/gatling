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
package io.gatling.core.util

object RoundRobin {

	def apply[T](values: Array[T]) = values.length match {
		case 0 => Iterator.empty
		case 1 =>
			new Iterator[T] {
				val hasNext = true
				val next = values(0)
			}
		case _ =>
			new Iterator[T] {
				val counter = new CyclicCounter(values.length)
				val hasNext = true
				def next() = values(counter.nextVal)
			}
	}
}
