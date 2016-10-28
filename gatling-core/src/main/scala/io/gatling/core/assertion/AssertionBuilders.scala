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
package io.gatling.core.assertion

import io.gatling.commons.stats.assertion._
import io.gatling.core.config.GatlingConfiguration

class AssertionWithPath(path: AssertionPath)(implicit configuration: GatlingConfiguration) {

  def responseTime = new AssertionWithPathAndTimeMetric(path, ResponseTime)
  def allRequests = new AssertionWithPathAndCountMetric(path, AllRequests)
  def failedRequests = new AssertionWithPathAndCountMetric(path, FailedRequests)
  def successfulRequests = new AssertionWithPathAndCountMetric(path, SuccessfulRequests)
  def requestsPerSec = new AssertionWithPathAndTarget[Int](path, MeanRequestsPerSecondTarget)
}

class AssertionWithPathAndTimeMetric(path: AssertionPath, metric: TimeMetric)(implicit configuration: GatlingConfiguration) {

  private def next(selection: TimeSelection) =
    new AssertionWithPathAndTarget[Int](path, TimeTarget(metric, selection))

  def min = next(Min)
  def max = next(Max)
  def mean = next(Mean)
  def stdDev = next(StandardDeviation)
  def percentile1 = next(Percentiles(configuration.charting.indicators.percentile1))
  def percentile2 = next(Percentiles(configuration.charting.indicators.percentile2))
  def percentile3 = next(Percentiles(configuration.charting.indicators.percentile3))
  def percentile4 = next(Percentiles(configuration.charting.indicators.percentile4))
}

class AssertionWithPathAndCountMetric(path: AssertionPath, metric: CountMetric) {

  def count = new AssertionWithPathAndTarget[Long](path, CountTarget(metric))
  def percent = new AssertionWithPathAndTarget[Double](path, PercentTarget(metric))
  def perMillion = new AssertionWithPathAndTarget[Double](path, PercentTarget(metric), adaptTargetValuesForPerMillion = true)
}

class AssertionWithPathAndTarget[T: Numeric](path: AssertionPath, target: Target, adaptTargetValuesForPerMillion: Boolean = false) {

  def next(condition: Condition) =
    Assertion(path, target, condition)

  val numeric = implicitly[Numeric[T]]

  private[this] def adaptTargetValue(value: Double): Double = if (adaptTargetValuesForPerMillion) value / 10000 else value

  def lessThan(threshold: T): Assertion = next(LessThan(adaptTargetValue(numeric.toDouble(threshold))))
  def greaterThan(threshold: T): Assertion = next(GreaterThan(adaptTargetValue(numeric.toDouble(threshold))))
  def between(min: T, max: T): Assertion = next(Between(adaptTargetValue(numeric.toDouble(min)), adaptTargetValue(numeric.toDouble(max))))
  def is(value: T): Assertion = next(Is(adaptTargetValue(numeric.toDouble(value))))
  def in(set: Set[T]): Assertion = next(In(set.map(v => adaptTargetValue(numeric.toDouble(v))).toList))
  def in(values: T*): Assertion = in(values.toSet)
}
