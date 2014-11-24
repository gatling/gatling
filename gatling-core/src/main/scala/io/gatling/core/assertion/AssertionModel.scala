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
package io.gatling.core.assertion

import com.dongxiguo.fastring.Fastring
import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.assertion.AssertionTags._

sealed abstract class Serialized(val serialized: Fastring)

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
  extends Serialized(serialize(AssertionTag, PathTag, path, TargetTag, target, ConditionTag, condition))

// -------------- //
// -- Path ADT -- //
// -------------- //

sealed abstract class Path(serialized: Fastring) extends Serialized(serialized)
case object Global extends Path(serialize(GlobalTag))
case object ForAll extends Path(serialize(ForAllTag))
case class Details(parts: List[String]) extends Path(serialize(string2Serialized(DetailsTag) +: parts.map(string2Serialized)))

// ---------------- //
// -- Metric ADT -- //
// ---------------- //

sealed abstract class Metric(serialized: Fastring) extends Serialized(serialized)
sealed abstract class TimeMetric(serialized: Fastring) extends Metric(serialized)
sealed abstract class CountMetric(serialized: Fastring) extends Metric(serialized)

case object AllRequests extends CountMetric(serialize(AllRequestsTag))
case object FailedRequests extends CountMetric(serialize(FailedRequestsTag))
case object SuccessfulRequests extends CountMetric(serialize(SuccessfulRequestsTag))
case object ResponseTime extends TimeMetric(serialize(ResponseTimeTag))

// ------------------- //
// -- Selection ADT -- //
// ------------------- //

sealed abstract class Selection(serialized: Fastring) extends Serialized(serialized)
sealed abstract class TimeSelection(serialized: Fastring) extends Selection(serialized)
sealed abstract class CountSelection(serialized: Fastring) extends Selection(serialized)

case object Count extends CountSelection(serialize(CountTag))
case object Percent extends CountSelection(serialize(PercentTag))
case object Min extends TimeSelection(serialize(MinTag))
case object Max extends TimeSelection(serialize(MaxTag))
case object Mean extends TimeSelection(serialize(MeanTag))
case object StandardDeviation extends TimeSelection(serialize(StandardDeviationTag))
case object Percentiles1 extends TimeSelection(serialize(Percentiles1Tag))
case object Percentiles2 extends TimeSelection(serialize(Percentiles2Tag))
case object Percentiles3 extends TimeSelection(serialize(Percentiles3Tag))
case object Percentiles4 extends TimeSelection(serialize(Percentiles4Tag))

// ---------------- //
// -- Target ADT -- //
// ---------------- //

sealed abstract class Target(serialized: Fastring) extends Serialized(serialized)
case class CountTarget(metric: CountMetric, selection: CountSelection) extends Target(serialize(metric, selection))
case class TimeTarget(metric: TimeMetric, selection: TimeSelection) extends Target(serialize(metric, selection))
case object MeanRequestsPerSecondTarget extends Target(serialize(MeanRequestsPerSecondTag))

// ------------------- //
// -- Condition ADT -- //
// ------------------- //

sealed abstract class Condition(serialized: Fastring) extends Serialized(serialized)
case class LessThan(value: Int) extends Condition(serialize(LessThanTag, value))
case class GreaterThan(value: Int) extends Condition(serialize(GreaterThanTag, value))
case class Is(value: Int) extends Condition(serialize(IsTag, value))
case class Between(lowerBound: Int, upperBound: Int) extends Condition(serialize(BetweenTag, lowerBound, upperBound))
case class In(elements: List[Int]) extends Condition(serialize(string2Serialized(InTag) +: elements.map(int2Serialized)))
