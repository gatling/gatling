/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.assertion

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.NumberHelper._

trait Printable {
  def printable(configuration: GatlingConfiguration): String
}

// ------------------- //
// -- Assertion ADT -- //
// ------------------- //

case class Assertion(path: Path, target: Target, condition: Condition)

// -------------- //
// -- Path ADT -- //
// -------------- //

sealed trait Path extends Printable
case object Global extends Path {
  def printable(configuration: GatlingConfiguration) = "Global"
}
case object ForAll extends Path {
  def printable(configuration: GatlingConfiguration) = "For all requests"
}
case class Details(parts: List[String]) extends Path {
  def printable(configuration: GatlingConfiguration) =
    if (parts.isEmpty)
      Global.printable(configuration)
    else
      parts.mkString(" / ")
}

// ---------------- //
// -- Metric ADT -- //
// ---------------- //

sealed trait TimeMetric extends Printable
sealed trait CountMetric extends Printable

case object AllRequests extends CountMetric {
  def printable(configuration: GatlingConfiguration) = "all requests"
}
case object FailedRequests extends CountMetric {
  def printable(configuration: GatlingConfiguration) = "failed requests"
}
case object SuccessfulRequests extends CountMetric {
  def printable(configuration: GatlingConfiguration) = "successful requests"
}
case object ResponseTime extends TimeMetric {
  def printable(configuration: GatlingConfiguration) = "response time"
}

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed trait TimeSelection extends Printable
sealed trait CountSelection extends Printable

case object Count extends CountSelection {
  def printable(configuration: GatlingConfiguration) = "count"
}
case object Percent extends CountSelection {
  def printable(configuration: GatlingConfiguration) = "percentage"
}
case object PerMillion extends CountSelection {
  def printable(configuration: GatlingConfiguration) = "per_million"
}
case object Min extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = "min"
}
case object Max extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = "max"
}
case object Mean extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = "mean"
}
case object StandardDeviation extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = "standard deviation"
}
case object Percentiles1 extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile1.toRank} percentile"
}
case object Percentiles2 extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile2.toRank} percentile"
}
case object Percentiles3 extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile3.toRank} percentile"
}
case object Percentiles4 extends TimeSelection {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile4.toRank} percentile"
}

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed trait Target extends Printable
case class CountTarget(metric: CountMetric, selection: CountSelection) extends Target {
  def printable(configuration: GatlingConfiguration) = s"${selection.printable(configuration)} of ${metric.printable(configuration)}"
}
case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target {
  def printable(configuration: GatlingConfiguration) = s"${selection.printable(configuration)} of ${metric.printable(configuration)}"
}
case object MeanRequestsPerSecondTarget extends Target {
  def printable(configuration: GatlingConfiguration) = "mean requests per second"
}

// ------------------- //
// -- Condition ADT -- //
// ------------------- //

sealed trait Condition extends Printable {
  def values: List[Int]
}
case class LessThan(value: Int) extends Condition {
  def printable(configuration: GatlingConfiguration) = "is less than"
  override def values = List(value)
}
case class GreaterThan(value: Int) extends Condition {
  def printable(configuration: GatlingConfiguration) = "is greater than"
  override def values = List(value)
}
case class Is(value: Int) extends Condition {
  def printable(configuration: GatlingConfiguration) = "is"
  override def values = List(value)
}
case class Between(lowerBound: Int, upperBound: Int) extends Condition {
  def printable(configuration: GatlingConfiguration) = "is between"
  override def values = List(lowerBound, upperBound)
}
case class In(elements: List[Int]) extends Condition {
  def printable(configuration: GatlingConfiguration) = "is in"
  override def values = elements
}
