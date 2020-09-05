/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.util

import scala.collection.mutable

object Maps {

  implicit class PimpedMap[K, V](val map: Map[K, V]) extends AnyVal {

    def forceMapValues[V2](f: V => V2): Map[K, V2] =
      if (map.isEmpty) {
        Map.empty
      } else {
        map.map { case (k, v) => k -> f(v) }
      }
  }

  implicit class PimpedPairTraversableOnce[K, V](val iterable: TraversableOnce[(K, V)]) extends AnyVal {

    def groupByKey[K2](f: K => K2): mutable.Map[K2, mutable.ArrayBuffer[V]] = {
      val mm = new mutable.HashMap[K2, mutable.ArrayBuffer[V]]
      for ((k1, value) <- iterable) {
        val k2 = f(k1)
        if (mm.contains(k2)) {
          mm(k2) += value
        } else {
          val newEntry = new mutable.ArrayBuffer[V](1)
          newEntry += value
          mm.update(k2, newEntry)
        }
      }
      mm
    }
  }
}
