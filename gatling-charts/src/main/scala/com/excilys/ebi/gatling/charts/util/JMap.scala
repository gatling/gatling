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
package com.excilys.ebi.gatling.charts.util

import scala.collection.JavaConversions.mapAsScalaMap

class JMap[K, V](map: java.util.Map[K, V] = new java.util.HashMap[K, V]) {

	def get(key: K) = map.get(key)

	def getOrElse(key: K, defaultValue: => V) =
		if (map.containsKey(key)) map.get(key)
		else defaultValue

	def put(key: K, value: => V) { map.put(key, value) }

	def getOrElseUpdate(key: K, builder: => V) =
		if (map.containsKey(key)) map.get(key)
		else {
			val value = builder
			map.put(key, value)
			value
		}

	def putOrUpdate(key: K, value: => V, update: V => V) {
		if (map.containsKey(key)) map.put(key, update(map.get(key)))
		else map.put(key, value)
	}

	def containsKey(key: K) = map.containsKey(key)

	def toList = map.toList

	def values = map.values

	def isEmpty = map.isEmpty
}
