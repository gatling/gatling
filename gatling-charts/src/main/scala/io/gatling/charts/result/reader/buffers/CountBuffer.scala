/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable

import io.gatling.core.result.IntVsTimePlot
import com.tdunning.math.stats.TDigest

class CountBuffer {
  val map = mutable.Map.empty[Int, IntVsTimePlot]
  val digest = TDigest.createArrayDigest(100)

  def update(value: Int) {
    val current = map.getOrElse(value, IntVsTimePlot(value, 0))
    map.put(value, current.copy(value = current.value + 1))
    digest.add(value)
  }
}
