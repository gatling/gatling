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

package io.gatling.commons.shared.unstable.model.stats.assertion

import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.stats.assertion._

object AssertionValidator {

  private type StatsByStatus = Option[Status] => AssertionStatsRepository.Stats
  private final case class PathResolution(unfoldedAssertion: Assertion, statsByStatus: StatsByStatus)
}

final class AssertionValidator(repository: AssertionStatsRepository) {

  def validateAssertions(assertions: List[Assertion]): List[AssertionResult] =
    for {
      rawAssertion <- assertions
      result <- resolvePath(rawAssertion, repository) match {
        case Left(error) =>
          AssertionResult.ResolutionError(rawAssertion, error) :: Nil

        case Right(pathResolutions) =>
          pathResolutions.map { case AssertionValidator.PathResolution(unfoldedAssertion, statsByStatus) =>
            val (actualValue, success) = resolveTarget(unfoldedAssertion, statsByStatus)
            AssertionResult.Resolved(unfoldedAssertion, success = success, actualValue)
          }
      }
    } yield result

  private def resolvePath(assertion: Assertion, statsSource: AssertionStatsRepository): Either[String, List[AssertionValidator.PathResolution]] =
    assertion.path match {
      case AssertionPath.Global | AssertionPath.Details(Nil) =>
        Right(
          AssertionValidator.PathResolution(
            unfoldedAssertion = assertion,
            statsByStatus = statsSource.requestGeneralStats(Nil, None, _)
          ) :: Nil
        )

      case AssertionPath.ForAll =>
        Right(
          statsSource.allRequestPaths().map { case AssertionStatsRepository.StatsPath.Request(group, request) =>
            AssertionValidator.PathResolution(
              unfoldedAssertion = assertion.copy(path = AssertionPath.Details(group ::: request :: Nil)),
              statsByStatus = statsSource.requestGeneralStats(group, Some(request), _)
            )
          }
        )

      case AssertionPath.Details(parts) =>
        statsSource.findPathByParts(parts) match {
          case Some(AssertionStatsRepository.StatsPath.Request(group, request)) =>
            Right(
              AssertionValidator.PathResolution(
                unfoldedAssertion = assertion,
                statsByStatus = statsSource.requestGeneralStats(group, Some(request), _)
              ) :: Nil
            )

          case Some(AssertionStatsRepository.StatsPath.Group(group)) =>
            Right(
              AssertionValidator.PathResolution(
                unfoldedAssertion = assertion,
                // FIXME we need an Assertions API overhaul to be able to target the group duration metric as well
                statsByStatus = statsSource.groupCumulatedResponseTimeGeneralStats(group, _)
              ) :: Nil
            )

          case _ =>
            Left(s"Could not find stats matching assertion path $parts")
        }
    }

  private def resolveTarget(assertion: Assertion, stats: AssertionValidator.StatsByStatus): (Double, Boolean) = {
    val actualValue = assertion.target match {
      case Target.MeanRequestsPerSecond => stats(None).meanRequestsPerSec

      case Target.Count(metric) => resolveCountTargetActualValue(metric, stats).toDouble

      case Target.Percent(metric) => resolvePercentTargetActualValue(metric, stats)

      case Target.Time(metric, stat) => resolveTimeTargetActualValue(metric, stat, stats).toDouble
    }

    (actualValue, resolveCondition(assertion, actualValue))
  }

  private def resolveCountTargetActualValue(metric: CountMetric, stats: AssertionValidator.StatsByStatus): Long = {
    val resolvedStats = metric match {
      case CountMetric.AllRequests        => stats(None)
      case CountMetric.FailedRequests     => stats(Some(KO))
      case CountMetric.SuccessfulRequests => stats(Some(OK))
    }

    resolvedStats.count
  }

  private def resolvePercentTargetActualValue(metric: CountMetric, stats: AssertionValidator.StatsByStatus): Double = {
    val allCount = stats(None).count

    metric match {
      case CountMetric.SuccessfulRequests =>
        if (allCount == 0) {
          0
        } else {
          stats(Some(OK)).count.toDouble / allCount * 100
        }
      case CountMetric.FailedRequests =>
        if (allCount == 0) {
          100
        } else {
          stats(Some(KO)).count.toDouble / allCount * 100
        }
      case _ => 100
    }
  }

  private def resolveTimeTargetActualValue(metric: TimeMetric, stat: Stat, stats: AssertionValidator.StatsByStatus): Int = {
    val resolvedStats = metric match {
      case TimeMetric.ResponseTime => stats(None)
    }

    stat match {
      case Stat.Min               => resolvedStats.min
      case Stat.Max               => resolvedStats.max
      case Stat.Mean              => resolvedStats.mean
      case Stat.StandardDeviation => resolvedStats.stdDev
      case Stat.Percentile(value) => resolvedStats.percentile(value)
    }
  }

  private def resolveCondition(assertion: Assertion, actualValue: Double): Boolean =
    assertion.condition match {
      case Condition.Lt(upper)                    => actualValue < upper
      case Condition.Lte(upper)                   => actualValue <= upper
      case Condition.Gt(lower)                    => actualValue > lower
      case Condition.Gte(lower)                   => actualValue >= lower
      case Condition.Is(exactValue)               => actualValue == exactValue
      case Condition.Between(lower, upper, true)  => actualValue >= lower && actualValue <= upper
      case Condition.Between(lower, upper, false) => actualValue > lower && actualValue < upper
      case Condition.In(elements)                 => elements.contains(actualValue)
    }
}
