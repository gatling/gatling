/**
 * Copyright 2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.util.cache

import scala.collection.concurrent
import scala.collection.JavaConversions._

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap

object ThreadSafeCache {

  def apply[K, V](maxCapacity: Long): ThreadSafeCache[K, V] =
    new ThreadSafeCache[K, V](maxCapacity)
}
class ThreadSafeCache[K, V](maxCapacity: Long) {

  val cache: concurrent.Map[K, V] = {
    new ConcurrentLinkedHashMap.Builder[K, V]
      .maximumWeightedCapacity(maxCapacity)
      .build
  }

  def enabled = maxCapacity > 0

  def getOrElsePutIfAbsent(key: K, value: => V): V = cache.get(key) match {
    case Some(v) => v
    case None =>
      val v = value
      cache.putIfAbsent(key, v)
      v
  }
}
