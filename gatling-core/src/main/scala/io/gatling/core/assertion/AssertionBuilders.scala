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
  def requestsPerSec = new AssertionWithPathAndTarget(path, MeanRequestsPerSecondTarget)
}

class AssertionWithPathAndTimeMetric(path: AssertionPath, metric: TimeMetric)(implicit configuration: GatlingConfiguration) {

  private def next(selection: TimeSelection) =
    new AssertionWithPathAndTarget(path, TimeTarget(metric, selection))

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

  private def next(selection: CountSelection) =
    new AssertionWithPathAndTarget(path, CountTarget(metric, selection))

  def count = next(Count)
  def percent = next(Percent)
  def perMillion = next(PerMillion)
}

class AssertionWithPathAndTarget(path: AssertionPath, target: Target) {

  def next(condition: Condition) =
    Assertion(path, target, condition)

  def lessThan(threshold: Int) = next(LessThan(threshold))
  def greaterThan(threshold: Int) = next(GreaterThan(threshold))
  def between(min: Int, max: Int) = next(Between(min, max))
  def is(value: Int) = next(Is(value))
  def in(set: Set[Int]) = next(In(set.toList))
  def in(values: Int*) = next(In(values.toSet.toList))
}
