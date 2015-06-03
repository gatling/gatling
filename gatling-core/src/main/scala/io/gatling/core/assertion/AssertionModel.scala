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

import io.gatling.core.util.NumberHelper._

trait Printable {
  def printable: String
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
  def printable = "Global"
}
case object ForAll extends Path {
  def printable = "For all requests"
}
case class Details(parts: List[String]) extends Path {
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
  def printable = "all requests"
}
case object FailedRequests extends CountMetric {
  def printable = "failed requests"
}
case object SuccessfulRequests extends CountMetric {
  def printable = "successful requests"
}
case object ResponseTime extends TimeMetric {
  def printable = "response time"
}

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed trait TimeSelection extends Printable
sealed trait CountSelection extends Printable

case object Count extends CountSelection {
  def printable = "count"
}
case object Percent extends CountSelection {
  def printable = "percentage"
}
case object PerMillion extends CountSelection {
  def printable = "per_million"
}
case object Min extends TimeSelection {
  def printable = "min"
}
case object Max extends TimeSelection {
  def printable = "max"
}
case object Mean extends TimeSelection {
  def printable = "mean"
}
case object StandardDeviation extends TimeSelection {
  def printable = "standard deviation"
}
case class Percentiles(value: Double) extends TimeSelection {
  def printable = s"${value.toRank} percentile"
}

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed trait Target extends Printable
case class CountTarget(metric: CountMetric, selection: CountSelection) extends Target {
  def printable = s"${selection.printable} of ${metric.printable}"
}
case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target {
  def printable = s"${selection.printable} of ${metric.printable}"
}
case object MeanRequestsPerSecondTarget extends Target {
  def printable = "mean requests per second"
}

// ------------------- //
// -- Condition ADT -- //
// ------------------- //

sealed trait Condition extends Printable {
  def values: List[Int]
}
case class LessThan(value: Int) extends Condition {
  def printable = "is less than"
  override def values = List(value)
}
case class GreaterThan(value: Int) extends Condition {
  def printable = "is greater than"
  override def values = List(value)
}
case class Is(value: Int) extends Condition {
  def printable = "is"
  override def values = List(value)
}
case class Between(lowerBound: Int, upperBound: Int) extends Condition {
  def printable = "is between"
  override def values = List(lowerBound, upperBound)
}
case class In(elements: List[Int]) extends Condition {
  def printable = "is in"
  override def values = elements
}
