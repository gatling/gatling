/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.metrics

import scala.collection.mutable

private[metrics] object MetricSeries {
  private val sanitizeStringMemo = mutable.Map.empty[String, String]
  def sanitizeString(s: String) = sanitizeStringMemo.getOrElseUpdate(s, s.replace(' ', '_').replace('.', '-').replace('\\', '-'))

  def metricSeries(metricSeries: String) = new MetricSeries(List(sanitizeString(metricSeries)))
  def metricSeries(seriesTree: List[String]) = new MetricSeries(seriesTree.map(sanitizeString))
}

private[metrics] case class MetricSeries private (seriesRootTree: List[String]) {
  import MetricSeries.sanitizeString
  def add(seriesNode: String) = new MetricSeries(sanitizeString(seriesNode) :: seriesRootTree)
  def add(seriesTree: MetricSeries) = new MetricSeries(seriesTree.seriesRootTree.map(sanitizeString) ::: seriesRootTree)
  def bucket = seriesRootTree.reverse.mkString(".")
}
