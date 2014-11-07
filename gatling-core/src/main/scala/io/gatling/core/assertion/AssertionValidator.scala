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

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.{ GroupStatsPath, RequestStatsPath, StatsPath }
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.reader.{ GeneralStats, DataReader }
import io.gatling.core.util.NumberHelper.RichDouble
import io.gatling.core.validation._

object AssertionValidator {

  type ValidatedRequestPath = Validation[Option[Status] => GeneralStats]
  type StatsByStatus = Option[Status] => GeneralStats

  val Percentile1 = configuration.charting.indicators.percentile1.toRank
  val Percentile2 = configuration.charting.indicators.percentile2.toRank

  private case class AssertionResult(result: Boolean, message: String)
  private case class ResolvedMetric(stats: GeneralStats, message: String)
  private case class ResolvedSelection(value: Double, message: String)

  def validateAssertions(dataReader: DataReader): Boolean =
    dataReader.assertions.foldLeft(true) { (isValid, assertion) =>
      val assertionResult = validateAssertion(assertion, dataReader)
      println(s"${assertionResult.message} : ${assertionResult.result}")
      isValid && assertionResult.result
    }

  private def validateAssertion(assertion: Assertion, dataReader: DataReader) = assertion.path match {
    case Global =>
      resolveTarget(assertion, status => dataReader.requestGeneralStats(None, None, status), "Global")

    case Details(parts) if parts.isEmpty =>
      resolveTarget(assertion, status => dataReader.requestGeneralStats(None, None, status), "Global")

    case Details(parts) =>
      val generalStats: ValidatedRequestPath = findPath(parts, dataReader) match {
        case None =>
          Failure(s"Could not find stats matching assertion path $parts")

        case Some(RequestStatsPath(request, group)) =>
          Success(status => dataReader.requestGeneralStats(Some(request), group, status))

        case Some(GroupStatsPath(group)) =>
          Success(status => dataReader.requestGeneralStats(None, Some(group), status))
      }
      generalStats match {
        case Success(stats) => resolveTarget(assertion, stats, parts.mkString(" / "))
        case Failure(msg)   => AssertionResult(false, msg)
      }
  }

  private def findPath(parts: List[String], dataReader: DataReader): Option[StatsPath] =
    dataReader.statsPaths.find { statsPath =>
      val path = statsPath match {
        case RequestStatsPath(request, group) =>
          group.map(_.hierarchy :+ request).getOrElse(List(request))

        case GroupStatsPath(group) =>
          group.hierarchy
      }
      path == parts
    }

  private def resolveTarget(assertion: Assertion, stats: StatsByStatus, path: String) = assertion.target match {
    case MeanRequestsPerSecondTarget =>
      val selection = stats(None).meanRequestsPerSec
      resolveCondition(assertion, selection, s"$path: mean requests per second")

    case target: CountTarget =>
      resolveCountTarget(assertion, target, stats, path)

    case target: TimeTarget =>
      resolveTimeTarget(assertion, target, stats, path)
  }

  private def resolveCountTarget(assertion: Assertion, target: CountTarget, stats: StatsByStatus, path: String) = {
    val resolvedMetric = target.metric match {
      case AllRequests        => ResolvedMetric(stats(None), "all requests")
      case FailedRequests     => ResolvedMetric(stats(Some(KO)), "failed requests")
      case SuccessfulRequests => ResolvedMetric(stats(Some(OK)), "successful requests")
    }
    val resolvedSelection = target.selection match {
      case Count   => ResolvedSelection(resolvedMetric.stats.count.toDouble, "count")
      case Percent => ResolvedSelection(resolvedMetric.stats.count.toDouble / stats(None).count * 100, "percentage")
    }
    resolveCondition(assertion, resolvedSelection.value, s"$path: ${resolvedSelection.message} of ${resolvedMetric.message}")
  }

  private def resolveTimeTarget(assertion: Assertion, target: TimeTarget, stats: StatsByStatus, path: String) = {
    val resolvedMetric = target.metric match {
      case ResponseTime => ResolvedMetric(stats(None), "response time")
    }
    val resolvedSelection = target.selection match {
      case Min               => ResolvedSelection(resolvedMetric.stats.min, "min")
      case Max               => ResolvedSelection(resolvedMetric.stats.max, "max")
      case Mean              => ResolvedSelection(resolvedMetric.stats.mean, "mean")
      case StandardDeviation => ResolvedSelection(resolvedMetric.stats.stdDev, "standard deviation")
      case Percentiles1      => ResolvedSelection(resolvedMetric.stats.percentile1, s"$Percentile1 percentile")
      case Percentiles2      => ResolvedSelection(resolvedMetric.stats.percentile2, s"$Percentile2 percentile")
    }
    resolveCondition(assertion, resolvedSelection.value, s"$path: ${resolvedSelection.message} of ${resolvedMetric.message}")
  }

  private def resolveCondition(assertion: Assertion, value: Double, message: String) =
    assertion.condition match {
      case LessThan(upper)       => AssertionResult(value <= upper, s"$message is less than $upper")
      case GreaterThan(lower)    => AssertionResult(lower <= value, s"$message is greater than $lower")
      case Is(exactValue)        => AssertionResult(exactValue == value, s"$message is $exactValue")
      case Between(lower, upper) => AssertionResult(lower <= value && value <= upper, s"$message is between $lower and $upper")
      case In(elements)          => AssertionResult(elements contains value, s"$message is in $elements")
    }
}
