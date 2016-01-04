/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.commons.stats.assertion

import io.gatling.commons.util.NumberHelper._

trait Printable {
  def printable: String
}

// ------------------- //
// -- Assertion ADT -- //
// ------------------- //

case class Assertion(path: AssertionPath, target: Target, condition: Condition)

// -------------- //
// -- Path ADT -- //
// -------------- //

sealed trait AssertionPath extends Printable
case object Global extends AssertionPath {
  val printable = "Global"
}
case object ForAll extends AssertionPath {
  val printable = "For all requests"
}
case class Details(parts: List[String]) extends AssertionPath {
  def printable =
    if (parts.isEmpty)
      Global.printable
    else
      parts.mkString(" / ")
}

// ---------------- //
// -- Metric ADT -- //
// ---------------- //

sealed trait TimeMetric extends Printable
sealed trait CountMetric extends Printable

case object AllRequests extends CountMetric {
  val printable = "all requests"
}
case object FailedRequests extends CountMetric {
  val printable = "failed requests"
}
case object SuccessfulRequests extends CountMetric {
  val printable = "successful requests"
}
case object ResponseTime extends TimeMetric {
  val printable = "response time"
}

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed trait TimeSelection extends Printable
sealed trait CountSelection extends Printable

case object Count extends CountSelection {
  val printable = "count"
}
case object Percent extends CountSelection {
  val printable = "percentage"
}
case object PerMillion extends CountSelection {
  val printable = "per_million"
}
case object Min extends TimeSelection {
  val printable = "min"
}
case object Max extends TimeSelection {
  val printable = "max"
}
case object Mean extends TimeSelection {
  val printable = "mean"
}
case object StandardDeviation extends TimeSelection {
  val printable = "standard deviation"
}
case class Percentiles(value: Double) extends TimeSelection {
  val printable = s"${value.toRank} percentile"
}

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed trait Target extends Printable
case class CountTarget(metric: CountMetric, selection: CountSelection) extends Target {
  val printable = s"${selection.printable} of ${metric.printable}"
}
case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target {
  val printable = s"${selection.printable} of ${metric.printable}"
}
case object MeanRequestsPerSecondTarget extends Target {
  val printable = "mean requests per second"
}

// ------------------- //
// -- Condition ADT -- //
// ------------------- //

sealed trait Condition extends Printable {
  def values: List[Int]
}
case class LessThan(value: Int) extends Condition {
  val printable = "is less than"
  override def values = List(value)
}
case class GreaterThan(value: Int) extends Condition {
  val printable = "is greater than"
  override def values = List(value)
}
case class Is(value: Int) extends Condition {
  val printable = "is"
  override def values = List(value)
}
case class Between(lowerBound: Int, upperBound: Int) extends Condition {
  val printable = "is between"
  override def values = List(lowerBound, upperBound)
}
case class In(elements: List[Int]) extends Condition {
  val printable = "is in"
  override def values = elements
}
