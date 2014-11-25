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

  type ValidatedRequestPath = Validation[Option[Status] => List[GeneralStats]]
  type StatsByStatus = Option[Status] => List[GeneralStats]

  val Percentile1 = configuration.charting.indicators.percentile1.toRank
  val Percentile2 = configuration.charting.indicators.percentile2.toRank
  val Percentile3 = configuration.charting.indicators.percentile3.toRank
  val Percentile4 = configuration.charting.indicators.percentile4.toRank

  private case class ResolvedMetric(stats: List[GeneralStats], message: String)
  private case class ResolvedSelection(value: List[Int], message: String)

  def validateAssertions(dataReader: DataReader): List[AssertionResult] =
    dataReader.assertions.map(validateAssertion(_, dataReader))

  private def validateAssertion(assertion: Assertion, dataReader: DataReader) = assertion.path match {
    case Global =>
      resolveTarget(assertion, status => List(dataReader.requestGeneralStats(None, None, status)), "Global")

    case ForAll =>
      val paths = dataReader.statsPaths.collect { case path: RequestStatsPath => path }
      val statsList: StatsByStatus = status => paths.map {
        case RequestStatsPath(request, group) =>
          dataReader.requestGeneralStats(Some(request), group, status)
      }
      resolveTarget(assertion, statsList, "For all requests")

    case Details(parts) if parts.isEmpty =>
      resolveTarget(assertion, status => List(dataReader.requestGeneralStats(None, None, status)), "Global")

    case Details(parts) =>
      val generalStats: ValidatedRequestPath = findPath(parts, dataReader) match {
        case None =>
          Failure(s"Could not find stats matching assertion path $parts")

        case Some(RequestStatsPath(request, group)) =>
          Success(status => List(dataReader.requestGeneralStats(Some(request), group, status)))

        case Some(GroupStatsPath(group)) =>
          Success(status => List(dataReader.requestGeneralStats(None, Some(group), status)))
      }
      generalStats match {
        case Success(stats) => resolveTarget(assertion, stats, parts.mkString(" / "))
        case Failure(msg)   => AssertionResult(result = false, msg)
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
      val selection = stats(None).map(_.meanRequestsPerSec.toInt)
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
      case Count => ResolvedSelection(resolvedMetric.stats.map(_.count), "count")
      case Percent =>
        val metricCountsAndAllCounts = resolvedMetric.stats.map(_.count).zip(stats(None).map(_.count))
        val percentages = metricCountsAndAllCounts.map { case (metricCount, allCount) => metricCount.toDouble / allCount * 100 }
        ResolvedSelection(percentages.map(_.toInt), "percentage")
    }
    resolveCondition(assertion, resolvedSelection.value, s"$path: ${resolvedSelection.message} of ${resolvedMetric.message}")
  }

  private def resolveTimeTarget(assertion: Assertion, target: TimeTarget, stats: StatsByStatus, path: String) = {
    val resolvedMetric = target.metric match {
      case ResponseTime => ResolvedMetric(stats(None), "response time")
    }
    val resolvedSelection = target.selection match {
      case Min               => ResolvedSelection(resolvedMetric.stats.map(_.min), "min")
      case Max               => ResolvedSelection(resolvedMetric.stats.map(_.max), "max")
      case Mean              => ResolvedSelection(resolvedMetric.stats.map(_.mean), "mean")
      case StandardDeviation => ResolvedSelection(resolvedMetric.stats.map(_.stdDev), "standard deviation")
      case Percentiles1      => ResolvedSelection(resolvedMetric.stats.map(_.percentile1), s"$Percentile1 percentile")
      case Percentiles2      => ResolvedSelection(resolvedMetric.stats.map(_.percentile2), s"$Percentile2 percentile")
      case Percentiles3      => ResolvedSelection(resolvedMetric.stats.map(_.percentile3), s"$Percentile3 percentile")
      case Percentiles4      => ResolvedSelection(resolvedMetric.stats.map(_.percentile4), s"$Percentile4 percentile")
    }
    resolveCondition(assertion, resolvedSelection.value, s"$path: ${resolvedSelection.message} of ${resolvedMetric.message}")
  }

  private def resolveCondition(assertion: Assertion, values: List[Int], message: String) =
    assertion.condition match {
      case LessThan(upper)       => AssertionResult(values.forall(_ <= upper), s"$message is less than $upper")
      case GreaterThan(lower)    => AssertionResult(values.forall(_ >= lower), s"$message is greater than $lower")
      case Is(exactValue)        => AssertionResult(values.forall(_ == exactValue), s"$message is $exactValue")
      case Between(lower, upper) => AssertionResult(values.forall(_.between(lower, upper)), s"$message is between $lower and $upper")
      case In(elements)          => AssertionResult(values.forall(elements contains), s"$message is in $elements")
    }

  private implicit class RichInt(val int: Int) extends AnyVal {
    def between(lower: Int, upper: Int) = lower <= int && int <= upper
  }
}
