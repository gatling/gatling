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

import com.dongxiguo.fastring.Fastring
import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.assertion.AssertionTags._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.NumberHelper._

sealed abstract class Serialized(val serialized: Fastring)

trait Printable {
  def printable(configuration: GatlingConfiguration): String
}

private object Serialized {
  implicit def string2Serialized(s: String): Serialized = new Serialized(fast"$s") {}
  implicit def int2Serialized(i: Int): Serialized = new Serialized(fast"$i") {}

  def serialize(parts: List[Serialized]): Fastring = serialize(parts: _*)
  def serialize(parts: Serialized*): Fastring = parts.map(_.serialized).mkFastring("\t")
}

import Serialized._

// ------------------- //
// -- Assertion ADT -- //
// ------------------- //

case class Assertion(path: Path, target: Target, condition: Condition)
  extends Serialized(serialize(PathTag, path, TargetTag, target, ConditionTag, condition))

// -------------- //
// -- Path ADT -- //
// -------------- //

sealed abstract class Path(serialized: Fastring) extends Serialized(serialized) with Printable
case object Global extends Path(serialize(GlobalTag)) {
  def printable(configuration: GatlingConfiguration) = "Global"
}
case object ForAll extends Path(serialize(ForAllTag)) {
  def printable(configuration: GatlingConfiguration) = "For all requests"
}
case class Details(parts: List[String]) extends Path(serialize(string2Serialized(DetailsTag) +: parts.map(string2Serialized))) {
  def printable(configuration: GatlingConfiguration) =
    if (parts.isEmpty)
      Global.printable(configuration)
    else
      parts.mkString(" / ")
}

// ---------------- //
// -- Metric ADT -- //
// ---------------- //

sealed abstract class Metric(serialized: Fastring) extends Serialized(serialized) with Printable
sealed abstract class TimeMetric(serialized: Fastring) extends Metric(serialized)
sealed abstract class CountMetric(serialized: Fastring) extends Metric(serialized)

case object AllRequests extends CountMetric(serialize(AllRequestsTag)) {
  def printable(configuration: GatlingConfiguration) = "all requests"
}
case object FailedRequests extends CountMetric(serialize(FailedRequestsTag)) {
  def printable(configuration: GatlingConfiguration) = "failed requests"
}
case object SuccessfulRequests extends CountMetric(serialize(SuccessfulRequestsTag)) {
  def printable(configuration: GatlingConfiguration) = "successful requests"
}
case object ResponseTime extends TimeMetric(serialize(ResponseTimeTag)) {
  def printable(configuration: GatlingConfiguration) = "response time"
}

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed abstract class Selection(serialized: Fastring) extends Serialized(serialized) with Printable
sealed abstract class TimeSelection(serialized: Fastring) extends Selection(serialized)
sealed abstract class CountSelection(serialized: Fastring) extends Selection(serialized)

case object Count extends CountSelection(serialize(CountTag)) {
  def printable(configuration: GatlingConfiguration) = "count"
}
case object Percent extends CountSelection(serialize(PercentTag)) {
  def printable(configuration: GatlingConfiguration) = "percentage"
}
case object Min extends TimeSelection(serialize(MinTag)) {
  def printable(configuration: GatlingConfiguration) = "min"
}
case object Max extends TimeSelection(serialize(MaxTag)) {
  def printable(configuration: GatlingConfiguration) = "max"
}
case object Mean extends TimeSelection(serialize(MeanTag)) {
  def printable(configuration: GatlingConfiguration) = "mean"
}
case object StandardDeviation extends TimeSelection(serialize(StandardDeviationTag)) {
  def printable(configuration: GatlingConfiguration) = "standard deviation"
}
case object Percentiles1 extends TimeSelection(serialize(Percentiles1Tag)) {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile1.toRank} percentile"
}
case object Percentiles2 extends TimeSelection(serialize(Percentiles2Tag)) {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile2.toRank} percentile"
}
case object Percentiles3 extends TimeSelection(serialize(Percentiles3Tag)) {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile3.toRank} percentile"
}
case object Percentiles4 extends TimeSelection(serialize(Percentiles4Tag)) {
  def printable(configuration: GatlingConfiguration) = s"${configuration.charting.indicators.percentile4.toRank} percentile"
}

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed abstract class Target(serialized: Fastring) extends Serialized(serialized) with Printable
case class CountTarget(metric: CountMetric, selection: CountSelection) extends Target(serialize(metric, selection)) {
  def printable(configuration: GatlingConfiguration) = s"${selection.printable(configuration)} of ${metric.printable(configuration)}"
}
case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target(serialize(metric, selection)) {
  def printable(configuration: GatlingConfiguration) = s"${selection.printable(configuration)} of ${metric.printable(configuration)}"
}
case object MeanRequestsPerSecondTarget extends Target(serialize(MeanRequestsPerSecondTag)) {
  def printable(configuration: GatlingConfiguration) = "mean requests per second"
}

// ------------------- //
// -- Condition ADT -- //
// ------------------- //

sealed abstract class Condition(serialized: Fastring) extends Serialized(serialized) with Printable {
  def values: List[Int]
}
case class LessThan(value: Int) extends Condition(serialize(LessThanTag, value)) {
  def printable(configuration: GatlingConfiguration) = "is less than"
  override def values = List(value)
}
case class GreaterThan(value: Int) extends Condition(serialize(GreaterThanTag, value)) {
  def printable(configuration: GatlingConfiguration) = "is greater than"
  override def values = List(value)
}
case class Is(value: Int) extends Condition(serialize(IsTag, value)) {
  def printable(configuration: GatlingConfiguration) = "is"
  override def values = List(value)
}
case class Between(lowerBound: Int, upperBound: Int) extends Condition(serialize(BetweenTag, lowerBound, upperBound)) {
  def printable(configuration: GatlingConfiguration) = "is between"
  override def values = List(lowerBound, upperBound)
}
case class In(elements: List[Int]) extends Condition(serialize(string2Serialized(InTag) +: elements.map(int2Serialized))) {
  def printable(configuration: GatlingConfiguration) = "is in"
  override def values = elements
}
