/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.feeder

import scala.collection.immutable.{ AbstractMap, Map }

private[feeder] object ArrayBasedMap {
  def apply[K, V](keys: Array[K], values: Array[V]): ArrayBasedMap[K, V] =
    new ArrayBasedMap(keys, values, math.min(keys.length, values.length))
}

private[feeder] class ArrayBasedMap[K, +V](keys: Array[K], values: Array[V], override val size: Int) extends AbstractMap[K, V] with Map[K, V] with Serializable {

  override def +[V1 >: V](kv: (K, V1)) = throw new UnsupportedOperationException

  override def updated[V1 >: V](key: K, value: V1): Map[K, V1] = throw new UnsupportedOperationException

  override def get(key: K): Option[V] = {
    var i = 0
    while (i < size) {
      if (keys(i) == key) {
        return Some(values(i))
      }
      i += 1
    }
    None
  }

  override def iterator: Iterator[(K, V)] = new Iterator[(K, V)] {

    private var i = 0

    override def hasNext: Boolean =
      i < ArrayBasedMap.this.size

    override def next(): (K, V) = {
      val v = keys(i) -> values(i)
      i += 1
      v
    }
  }

  override def -(key: K) = throw new UnsupportedOperationException
}
