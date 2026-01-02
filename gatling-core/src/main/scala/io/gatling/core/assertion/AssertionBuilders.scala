/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.stats.assertion._
import io.gatling.core.config.GatlingConfiguration

final class AssertionWithPath(path: AssertionPath, configuration: GatlingConfiguration) {
  def responseTime: AssertionWithPathAndTimeMetric = new AssertionWithPathAndTimeMetric(path, TimeMetric.ResponseTime, configuration)
  def allRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, CountMetric.AllRequests)
  def failedRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, CountMetric.FailedRequests)
  def successfulRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, CountMetric.SuccessfulRequests)
  def requestsPerSec: AssertionWithPathAndTarget[Double] = new AssertionWithPathAndTarget[Double](path, Target.MeanRequestsPerSecond)
}

final class AssertionWithPathAndTimeMetric(path: AssertionPath, metric: TimeMetric, configuration: GatlingConfiguration) {
  private def next(selection: Stat) =
    new AssertionWithPathAndTarget[Int](path, Target.Time(metric, selection))

  def min: AssertionWithPathAndTarget[Int] = next(Stat.Min)
  def max: AssertionWithPathAndTarget[Int] = next(Stat.Max)
  def mean: AssertionWithPathAndTarget[Int] = next(Stat.Mean)
  def stdDev: AssertionWithPathAndTarget[Int] = next(Stat.StandardDeviation)
  def percentile1: AssertionWithPathAndTarget[Int] = percentile(configuration.reports.indicators.percentile1)
  def percentile2: AssertionWithPathAndTarget[Int] = percentile(configuration.reports.indicators.percentile2)
  def percentile3: AssertionWithPathAndTarget[Int] = percentile(configuration.reports.indicators.percentile3)
  def percentile4: AssertionWithPathAndTarget[Int] = percentile(configuration.reports.indicators.percentile4)
  def percentile(value: Double): AssertionWithPathAndTarget[Int] = next(Stat.Percentile(value))
}

final class AssertionWithPathAndCountMetric(path: AssertionPath, metric: CountMetric) {
  def count: AssertionWithPathAndTarget[Long] = new AssertionWithPathAndTarget[Long](path, Target.Count(metric))
  def percent: AssertionWithPathAndTarget[Double] = new AssertionWithPathAndTarget[Double](path, Target.Percent(metric))
}

final class AssertionWithPathAndTarget[T: Numeric](path: AssertionPath, target: Target) {
  def next(condition: Condition): Assertion =
    Assertion(path, target, condition)

  private val numeric = implicitly[Numeric[T]]

  def lt(threshold: T): Assertion = next(Condition.Lt(numeric.toDouble(threshold)))
  def lte(threshold: T): Assertion = next(Condition.Lte(numeric.toDouble(threshold)))
  def gt(threshold: T): Assertion = next(Condition.Gt(numeric.toDouble(threshold)))
  def gte(threshold: T): Assertion = next(Condition.Gte(numeric.toDouble(threshold)))
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def between(min: T, max: T, inclusive: Boolean = true): Assertion = next(Condition.Between(numeric.toDouble(min), numeric.toDouble(max), inclusive))
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def around(mean: T, plusOrMinus: T, inclusive: Boolean = true): Assertion =
    between(numeric.minus(mean, plusOrMinus), numeric.plus(mean, plusOrMinus), inclusive)
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def deviatesAround(mean: T, percentDeviation: Double, inclusive: Boolean = true): Assertion = {
    val plusOrMinus = numeric.fromInt((numeric.toDouble(mean) * percentDeviation).floor.toInt)
    around(mean, plusOrMinus, inclusive)
  }
  def is(value: T): Assertion = next(Condition.Is(numeric.toDouble(value)))
  def in(set: Set[T]): Assertion = next(Condition.In(set.map(numeric.toDouble).toList))
  def in(values: T*): Assertion = in(values.toSet)
}
