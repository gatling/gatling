/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

final case class Assertion(path: AssertionPath, target: Target, condition: Condition)

// -------------- //
// -- Path ADT -- //
// -------------- //

sealed abstract class AssertionPath(val printable: String) extends Printable with Product with Serializable
case object Global extends AssertionPath("Global")
case object ForAll extends AssertionPath("For all requests")
final case class Details(parts: List[String]) extends AssertionPath(if (parts.isEmpty) Global.printable else parts.mkString(" / "))

// ---------------- //
// -- Metric ADT -- //
// ---------------- //

sealed abstract class TimeMetric(val printable: String) extends Printable with Product with Serializable
sealed abstract class CountMetric(val printable: String) extends Printable with Product with Serializable

case object AllRequests extends CountMetric("all events")
case object FailedRequests extends CountMetric("failed events")
case object SuccessfulRequests extends CountMetric("successful events")
case object ResponseTime extends TimeMetric("response time")

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed abstract class TimeSelection(val printable: String) extends Printable with Product with Serializable

case object Min extends TimeSelection("min")
case object Max extends TimeSelection("max")
case object Mean extends TimeSelection("mean")
case object StandardDeviation extends TimeSelection("standard deviation")
final case class Percentiles(value: Double) extends TimeSelection(s"${value.toRank} percentile")

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed abstract class Target(val printable: String) extends Printable with Product with Serializable
final case class CountTarget(metric: CountMetric) extends Target(s"count of ${metric.printable}")
final case class PercentTarget(metric: CountMetric) extends Target(s"percentage of ${metric.printable}")
final case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target(s"${selection.printable} of ${metric.printable}")
case object MeanRequestsPerSecondTarget extends Target("mean requests per second")

// ------------------- //
// -- Condition ADT -- //
// ------------------- //
sealed abstract class Condition(val printable: String) extends Printable with Product with Serializable {
  def values: List[Double]
}
final case class Lte(value: Double) extends Condition("is less than or equal to") {
  override def values: List[Double] = List(value)
}
final case class Gte(value: Double) extends Condition("is greater than or equal to") {
  override def values = List(value)
}
final case class Lt(value: Double) extends Condition("is less than") {
  override def values: List[Double] = List(value)
}
final case class Gt(value: Double) extends Condition("is greater than") {
  override def values = List(value)
}
final case class Is(value: Double) extends Condition("is") {
  override def values = List(value)
}
final case class Between(lowerBound: Double, upperBound: Double, inclusive: Boolean) extends Condition("is between" + (if (inclusive) " inclusive" else "")) {
  override def values = List(lowerBound, upperBound)
}
final case class In(elements: List[Double]) extends Condition("is in") {
  override def values: List[Double] = elements
}
