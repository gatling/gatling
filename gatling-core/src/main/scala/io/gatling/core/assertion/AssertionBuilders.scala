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

package io.gatling.core.assertion

import io.gatling.commons.stats.assertion._
import io.gatling.core.config.GatlingConfiguration

class AssertionWithPath(path: AssertionPath, configuration: GatlingConfiguration) {

  def responseTime: AssertionWithPathAndTimeMetric = new AssertionWithPathAndTimeMetric(path, ResponseTime, configuration)
  def allRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, AllRequests)
  def failedRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, FailedRequests)
  def successfulRequests: AssertionWithPathAndCountMetric = new AssertionWithPathAndCountMetric(path, SuccessfulRequests)
  def requestsPerSec: AssertionWithPathAndTarget[Double] = new AssertionWithPathAndTarget[Double](path, MeanRequestsPerSecondTarget)
}

class AssertionWithPathAndTimeMetric(path: AssertionPath, metric: TimeMetric, configuration: GatlingConfiguration) {

  private def next(selection: TimeSelection) =
    new AssertionWithPathAndTarget[Int](path, TimeTarget(metric, selection))

  def min: AssertionWithPathAndTarget[Int] = next(Min)
  def max: AssertionWithPathAndTarget[Int] = next(Max)
  def mean: AssertionWithPathAndTarget[Int] = next(Mean)
  def stdDev: AssertionWithPathAndTarget[Int] = next(StandardDeviation)
  def percentile1: AssertionWithPathAndTarget[Int] = percentile(configuration.charting.indicators.percentile1)
  def percentile2: AssertionWithPathAndTarget[Int] = percentile(configuration.charting.indicators.percentile2)
  def percentile3: AssertionWithPathAndTarget[Int] = percentile(configuration.charting.indicators.percentile3)
  def percentile4: AssertionWithPathAndTarget[Int] = percentile(configuration.charting.indicators.percentile4)
  def percentile(value: Double): AssertionWithPathAndTarget[Int] = next(Percentiles(value))
}

class AssertionWithPathAndCountMetric(path: AssertionPath, metric: CountMetric) {

  def count: AssertionWithPathAndTarget[Long] = new AssertionWithPathAndTarget[Long](path, CountTarget(metric))
  def percent: AssertionWithPathAndTarget[Double] = new AssertionWithPathAndTarget[Double](path, PercentTarget(metric))
}

class AssertionWithPathAndTarget[T: Numeric](path: AssertionPath, target: Target) {

  def next(condition: Condition): Assertion =
    Assertion(path, target, condition)

  private val numeric = implicitly[Numeric[T]]

  def lt(threshold: T): Assertion = next(Lt(numeric.toDouble(threshold)))
  def lte(threshold: T): Assertion = next(Lte(numeric.toDouble(threshold)))
  def gt(threshold: T): Assertion = next(Gt(numeric.toDouble(threshold)))
  def gte(threshold: T): Assertion = next(Gte(numeric.toDouble(threshold)))
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def between(min: T, max: T, inclusive: Boolean = true): Assertion = next(Between(numeric.toDouble(min), numeric.toDouble(max), inclusive))
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def around(mean: T, plusOrMinus: T, inclusive: Boolean = true): Assertion =
    between(numeric.minus(mean, plusOrMinus), numeric.plus(mean, plusOrMinus), inclusive)
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def deviatesAround(target: T, percentDeviationThreshold: Double, inclusive: Boolean = true): Assertion = {
    val margin = numeric.fromInt((numeric.toDouble(target) * percentDeviationThreshold).floor.toInt)
    between(numeric.minus(target, margin), numeric.plus(target, margin), inclusive)
  }
  def is(value: T): Assertion = next(Is(numeric.toDouble(value)))
  def in(set: Set[T]): Assertion = next(In(set.map(numeric.toDouble).toList))
  def in(values: T*): Assertion = in(values.toSet)

}
