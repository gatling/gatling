/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.shared.model.assertion

import io.gatling.shared.util.NumberHelper._

private[gatling] object AssertionMessage {

  private def printableCountMetric(countMetric: CountMetric): String = countMetric match {
    case CountMetric.AllRequests        => "all events"
    case CountMetric.FailedRequests     => "failed events"
    case CountMetric.SuccessfulRequests => "successful events"
  }

  private def printablePath(path: AssertionPath): String = path match {
    case AssertionPath.Global                       => "Global"
    case AssertionPath.Details(parts: List[String]) => if (parts.isEmpty) "Global" else parts.mkString(" / ")
    case AssertionPath.ForAll                       => throw new UnsupportedOperationException("Can't generate message for ForAll")
  }

  private def printableTarget(target: Target): String = target match {
    case Target.Count(countMetric)   => s"count of ${printableCountMetric(countMetric)}"
    case Target.Percent(countMetric) => s"percentage of ${printableCountMetric(countMetric)}"
    case Target.Time(timeMetric, stat) =>
      val printableStat = stat match {
        case Stat.Min               => "min"
        case Stat.Max               => "max"
        case Stat.Mean              => "mean"
        case Stat.StandardDeviation => "standard deviation"
        case Stat.Percentile(value) => s"${value.toRank} percentile"
      }

      val printableTimeMetric: String = timeMetric match {
        case TimeMetric.ResponseTime => "response time"
      }

      s"$printableStat of $printableTimeMetric"
    case Target.MeanRequestsPerSecond => "mean requests per second"
  }

  private def printableCondition(condition: Condition): String = condition match {
    case Condition.Lte(expected)                              => s"is less than or equal to $expected"
    case Condition.Gte(expected)                              => s"is greater than or equal to $expected"
    case Condition.Lt(expected)                               => s"is less than $expected"
    case Condition.Gt(expected)                               => s"is greater than $expected"
    case Condition.Is(expected)                               => s"is $expected"
    case Condition.Between(lowerBound, upperBound, inclusive) => s"is between $lowerBound and $upperBound" + (if (inclusive) " inclusive" else "")
    case Condition.In(expected)                               => s"is in $expected"
  }

  def message(assertion: Assertion): String =
    s"${printablePath(assertion.path)}: ${printableTarget(assertion.target)} ${printableCondition(assertion.condition)}"
}
