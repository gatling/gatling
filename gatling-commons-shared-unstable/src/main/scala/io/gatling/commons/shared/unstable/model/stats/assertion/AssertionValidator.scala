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

package io.gatling.commons.shared.unstable.model.stats.assertion

import io.gatling.commons.shared.unstable.model.stats
import io.gatling.commons.shared.unstable.model.stats._
import io.gatling.commons.stats._
import io.gatling.commons.stats.assertion._

object AssertionValidator {

  type StatsByStatus = Option[Status] => GeneralStats

  def validateAssertions(dataReader: GeneralStatsSource): List[AssertionResult] =
    dataReader.assertions.flatMap(validateAssertion(_, dataReader))

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def validateAssertion(assertion: Assertion, source: GeneralStatsSource): List[AssertionResult] = {

    val printablePath = assertion.path.printable

    assertion.path match {
      case Global =>
        List(resolveTarget(assertion, status => source.requestGeneralStats(None, None, status), printablePath))

      case ForAll =>
        val detailedAssertions = source.statsPaths.collect { case RequestStatsPath(request, group) =>
          assertion.copy(path = Details(group.map(_.hierarchy).getOrElse(Nil) ::: List(request)))
        }

        detailedAssertions.flatMap(validateAssertion(_, source))

      case Details(parts) if parts.isEmpty =>
        List(resolveTarget(assertion, status => source.requestGeneralStats(None, None, status), printablePath))

      case Details(parts) =>
        findPath(parts, source) match {
          case Some(RequestStatsPath(request, group)) =>
            List(resolveTarget(assertion, source.requestGeneralStats(Some(request), group, _), printablePath))

          case Some(GroupStatsPath(group)) =>
            List(resolveTarget(assertion, source.groupCumulatedResponseTimeGeneralStats(group, _), printablePath))

          case _ =>
            List(AssertionResult(assertion, result = false, s"Could not find stats matching assertion path $parts", None))
        }
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private def findPath(parts: List[String], source: GeneralStatsSource): Option[StatsPath] =
    source.statsPaths.find { statsPath =>
      val path = statsPath match {
        case RequestStatsPath(request, group) =>
          group.map(_.hierarchy :+ request).getOrElse(List(request))

        case GroupStatsPath(group) =>
          group.hierarchy
      }
      path == parts
    }

  private def resolveTarget(assertion: Assertion, stats: StatsByStatus, path: String) = {

    val printableTarget = assertion.target.printable

    assertion.target match {
      case MeanRequestsPerSecondTarget =>
        val actualValue = stats(None).meanRequestsPerSec
        resolveCondition(assertion, path, printableTarget, actualValue)

      case target: CountTarget =>
        val actualValue = resolveCountTargetActualValue(target, stats)
        resolveCondition(assertion, path, printableTarget, actualValue.toDouble)

      case target: PercentTarget =>
        val actualValue = resolvePercentTargetActualValue(target, stats)
        resolveCondition(assertion, path, printableTarget, actualValue)

      case target: TimeTarget =>
        val actualValue = resolveTimeTargetActualValue(target, stats)
        resolveCondition(assertion, path, printableTarget, actualValue)
    }
  }

  private def resolveCountTargetActualValue(target: CountTarget, stats: StatsByStatus): Long = {

    val resolvedStats = target.metric match {
      case AllRequests        => stats(None)
      case FailedRequests     => stats(Some(KO))
      case SuccessfulRequests => stats(Some(OK))
    }

    resolvedStats.count
  }

  private def resolvePercentTargetActualValue(target: PercentTarget, stats: StatsByStatus): Double = {

    val allCount = stats(None).count

    target.metric match {
      case SuccessfulRequests =>
        if (allCount == 0) {
          0
        } else {
          stats(Some(OK)).count.toDouble / allCount * 100
        }
      case FailedRequests =>
        if (allCount == 0) {
          100
        } else {
          stats(Some(KO)).count.toDouble / allCount * 100
        }
      case _ => 100
    }
  }

  private def resolveTimeTargetActualValue(target: TimeTarget, stats: StatsByStatus): Int = {

    val resolvedStats = target.metric match {
      case ResponseTime => stats(None)
    }

    target.selection match {
      case Min                => resolvedStats.min
      case Max                => resolvedStats.max
      case Mean               => resolvedStats.mean
      case StandardDeviation  => resolvedStats.stdDev
      case Percentiles(value) => resolvedStats.percentile(value)
    }
  }

  private def resolveCondition(assertion: Assertion, path: String, printableTarget: String, actualValue: Double): AssertionResult = {

    val (result, expectedValueMessage) =
      assertion.condition match {
        case Lt(upper)                    => (actualValue < upper, upper.toString)
        case Lte(upper)                   => (actualValue <= upper, upper.toString)
        case Gt(lower)                    => (actualValue > lower, lower.toString)
        case Gte(lower)                   => (actualValue >= lower, lower.toString)
        case Is(exactValue)               => (actualValue == exactValue, exactValue.toString)
        case Between(lower, upper, true)  => (actualValue >= lower && actualValue <= upper, s"$lower and $upper")
        case Between(lower, upper, false) => (actualValue > lower && actualValue < upper, s"$lower and $upper")
        case In(elements)                 => (elements.contains(actualValue), elements.toString)
      }

    stats.assertion.AssertionResult(assertion, result, s"$path: $printableTarget ${assertion.condition.printable} $expectedValueMessage", Some(actualValue))
  }
}
