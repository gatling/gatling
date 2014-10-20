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
package io.gatling.core.util

import collection.immutable.Queue
import collection.JavaConversions._

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap

package object cache {

  object Cache {

    def apply[K, V](maxCapacity: Int) = new Cache[K, V](Queue.empty, Map.empty, maxCapacity)
  }

  class Cache[K, V](queue: Queue[K], map: Map[K, V], maxCapacity: Int) {

    def +(kv: (K, V)): Cache[K, V] = {
      val (key, value) = kv
      add(key, value)
    }
    def add(key: K, value: V): Cache[K, V] = {
      if (map.contains(key))
        this

      else if (map.size == maxCapacity) {
        val (removedKey, newQueue) = queue.dequeue
        val newMap = map - removedKey + (key -> value)
        new Cache(newQueue.enqueue(key), newMap, maxCapacity)

      } else {
        val newQueue = queue.enqueue(key)
        val newMap = map + (key -> value)
        new Cache(newQueue, newMap, maxCapacity)
      }
    }

    def -(key: K): Cache[K, V] = remove(key)
    def remove(key: K): Cache[K, V] = {
      if (map.contains(key)) {
        val newQueue = queue.filter(_ != key)
        val newMap = map - key
        new Cache(newQueue, newMap, maxCapacity)

      } else
        this
    }

    def get(key: K): Option[V] = map.get(key)
  }

  type ThreadSafeCache[K, V] = collection.concurrent.Map[K, V]

  object ThreadSafeCache {

    def apply[K, V](maxCapacity: Long): ThreadSafeCache[K, V] =
      new ConcurrentLinkedHashMap.Builder[K, V]
        .maximumWeightedCapacity(maxCapacity)
        .build
  }

  implicit class RichThreadSafeCache[K, V](val underlying: ThreadSafeCache[K, V]) extends AnyVal {

    def getOrElsePutIfAbsent(key: K, value: => V): V = underlying.get(key) match {
      case Some(v) => v
      case None =>
        val v = value
        underlying.putIfAbsent(key, v)
        v
    }
  }
}
