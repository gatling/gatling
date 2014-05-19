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
package io.gatling.core

package result {

  case class Series[X](name: String, data: Seq[X], colors: List[String])
  case class IntVsTimePlot(time: Int, value: Int)
  case class PercentVsTimePlot(time: Int, value: Double) {
    def roundedUpValue: Double = (value * 100).toInt / 100.0
  }
  case class IntRangeVsTimePlot(time: Int, lower: Int, higher: Int)
  case class PieSlice(name: String, value: Double)
  case class ErrorStats(message: String, count: Int, totalCount: Int) {
    def percentage = count * 100.0 / totalCount
  }
}
