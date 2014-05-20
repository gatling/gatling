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

class CountBuffer {
  val counts = mutable.Map.empty[Int, Int]

  def update(time: Int) {
    val newCount = counts.get(time) match {
      case Some(count) => count + 1
      case None        => 1
    }
    counts.put(time, newCount)
  }

  def distribution: Iterable[IntVsTimePlot] = counts.map { case (time, count) => IntVsTimePlot(time, count) }
}
