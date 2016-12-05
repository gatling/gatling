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

import io.gatling.commons.stats._
import io.gatling.commons.validation.{ Failure, Success, Validation }

object AssertionValidator {

  type ValidatedRequestPath = Validation[Option[Status] => GeneralStats]
  type StatsByStatus = Option[Status] => GeneralStats

  def validateAssertions(dataReader: GeneralStatsSource): List[AssertionResult] =
    dataReader.assertions.flatMap(validateAssertion(_, dataReader))

  private def validateAssertion(assertion: Assertion, source: GeneralStatsSource): List[AssertionResult] = {

    val printablePath = assertion.path.printable

    assertion.path match {
      case Global =>
        List(resolveTarget(assertion, status => source.requestGeneralStats(None, None, status), printablePath))

      case ForAll =>
        val detailedAssertions = source.statsPaths.collect {
          case RequestStatsPath(request, group) =>
            assertion.copy(path = Details(group.map(_.hierarchy).getOrElse(Nil) ::: List(request)))
        }

        detailedAssertions.flatMap(validateAssertion(_, source))

      case Details(parts) if parts.isEmpty =>
        List(resolveTarget(assertion, status => source.requestGeneralStats(None, None, status), printablePath))

      case Details(parts) =>
        val generalStats: ValidatedRequestPath = findPath(parts, source) match {
          case None =>
            Failure(s"Could not find stats matching assertion path $parts")

          case Some(RequestStatsPath(request, group)) =>
            Success(status => source.requestGeneralStats(Some(request), group, status))

          case Some(GroupStatsPath(group)) =>
            Success(status => source.groupCumulatedResponseTimeGeneralStats(group, status))
        }
        generalStats match {
          case Success(stats) => List(resolveTarget(assertion, stats, printablePath))
          case Failure(msg)   => List(AssertionResult(assertion, result = false, msg, None))
        }
    }
  }

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
        resolveCondition(assertion, path, printableTarget, actualValue)

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

    val resolvedStats = target.metric match {
      case AllRequests        => stats(None)
      case FailedRequests     => stats(Some(KO))
      case SuccessfulRequests => stats(Some(OK))
    }

    val metricCount = resolvedStats.count
    val allCount = stats(None).count
    metricCount.toDouble / allCount * 100
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

  private def resolveCondition(assertion: Assertion, path: String, printableTarget: String, actualValue: Double) = {

    val printableCondition = assertion.condition.printable

      def assertionResult(result: Boolean, expectedValueMessage: Any) =
        AssertionResult(assertion, result, s"$path: $printableTarget $printableCondition $expectedValueMessage", Some(actualValue))

    assertion.condition match {
      case Lt(upper)                    => assertionResult(actualValue < upper, upper)
      case Lte(upper)                   => assertionResult(actualValue <= upper, upper)
      case Gt(lower)                    => assertionResult(actualValue > lower, lower)
      case Gte(lower)                   => assertionResult(actualValue >= lower, lower)
      case Is(exactValue)               => assertionResult(actualValue == exactValue, exactValue)
      case Between(lower, upper, true)  => assertionResult(actualValue > lower && actualValue < upper, s"$lower and $upper")
      case Between(lower, upper, false) => assertionResult(actualValue >= lower && actualValue <= upper, s"$lower and $upper")
      case In(elements)                 => assertionResult(elements.contains(actualValue), elements)
    }
  }
}
