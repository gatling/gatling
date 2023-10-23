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

import io.gatling.commons.shared.unstable.model.stats._
import io.gatling.commons.stats._
import io.gatling.commons.stats.assertion._

final class AssertionValidator(statsSource: GeneralStatsSource) {

  private type StatsByStatus = Option[Status] => GeneralStats

  def validateAssertions(assertions: List[Assertion]): List[AssertionResult] =
    for {
      rawAssertion <- assertions
      unfoldedAssertion <- unfold(rawAssertion, statsSource)
    } yield {
      resolvePath(unfoldedAssertion, statsSource) match {
        case Left(error) =>
          AssertionResult.ResolutionError(unfoldedAssertion, error)

        case Right(statsByStatus) =>
          val (actualValue, success) = resolveTarget(unfoldedAssertion, statsByStatus)
          AssertionResult.Resolved(unfoldedAssertion, success = success, actualValue)
      }
    }

  private def unfold(assertion: Assertion, source: GeneralStatsSource): List[Assertion] =
    assertion.path match {
      case AssertionPath.ForAll =>
        source.statsPaths.collect { case RequestStatsPath(request, group) =>
          assertion.copy(path = AssertionPath.Details(group.map(_.hierarchy).getOrElse(Nil) ::: List(request)))
        }
      case _ =>
        assertion :: Nil
    }

  private def resolvePath(assertion: Assertion, statsSource: GeneralStatsSource): Either[String, StatsByStatus] =
    assertion.path match {
      case AssertionPath.Global =>
        Right(statsSource.requestGeneralStats(None, None, _))

      case AssertionPath.Details(Nil) =>
        Right(statsSource.requestGeneralStats(None, None, _))

      case AssertionPath.Details(parts) =>
        findPath(parts, statsSource) match {
          case Some(RequestStatsPath(request, group)) =>
            Right(statsSource.requestGeneralStats(Some(request), group, _))

          case Some(GroupStatsPath(group)) =>
            Right(statsSource.groupCumulatedResponseTimeGeneralStats(group, _))

          case _ =>
            Left(s"Could not find stats matching assertion path $parts")
        }

      case unsupported =>
        throw new IllegalStateException(s"Unsupported assertion path $unsupported")
    }

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private def findPath(parts: List[String], statsSource: GeneralStatsSource): Option[StatsPath] =
    statsSource.statsPaths.find { statsPath =>
      val path = statsPath match {
        case RequestStatsPath(request, group) =>
          group.map(_.hierarchy :+ request).getOrElse(List(request))

        case GroupStatsPath(group) =>
          group.hierarchy
      }
      path == parts
    }

  private def resolveTarget(assertion: Assertion, stats: StatsByStatus): (Double, Boolean) = {
    val actualValue = assertion.target match {
      case Target.MeanRequestsPerSecond => stats(None).meanRequestsPerSec

      case target: Target.Count => resolveCountTargetActualValue(target, stats).toDouble

      case target: Target.Percent => resolvePercentTargetActualValue(target, stats)

      case target: Target.Time => resolveTimeTargetActualValue(target, stats).toDouble
    }

    (actualValue, resolveCondition(assertion, actualValue))
  }

  private def resolveCountTargetActualValue(target: Target.Count, stats: StatsByStatus): Long = {
    val resolvedStats = target.metric match {
      case CountMetric.AllRequests        => stats(None)
      case CountMetric.FailedRequests     => stats(Some(KO))
      case CountMetric.SuccessfulRequests => stats(Some(OK))
    }

    resolvedStats.count
  }

  private def resolvePercentTargetActualValue(target: Target.Percent, stats: StatsByStatus): Double = {
    val allCount = stats(None).count

    target.metric match {
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

  private def resolveTimeTargetActualValue(target: Target.Time, stats: StatsByStatus): Int = {
    val resolvedStats = target.metric match {
      case TimeMetric.ResponseTime => stats(None)
    }

    target.stat match {
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
