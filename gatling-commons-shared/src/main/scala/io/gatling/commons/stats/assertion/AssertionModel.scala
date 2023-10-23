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

package io.gatling.commons.stats.assertion

final case class Assertion(path: AssertionPath, target: Target, condition: Condition)

sealed trait AssertionPath extends Product with Serializable

object AssertionPath {
  case object Global extends AssertionPath

  case object ForAll extends AssertionPath

  final case class Details(parts: List[String]) extends AssertionPath
}

sealed trait TimeMetric extends Product with Serializable

object TimeMetric {
  case object ResponseTime extends TimeMetric
}

sealed trait CountMetric extends Product with Serializable

object CountMetric {
  case object AllRequests extends CountMetric

  case object FailedRequests extends CountMetric

  case object SuccessfulRequests extends CountMetric
}

sealed trait Stat extends Product with Serializable

object Stat {
  case object Min extends Stat
  case object Max extends Stat
  case object Mean extends Stat
  case object StandardDeviation extends Stat
  final case class Percentile(value: Double) extends Stat
}

sealed trait Target extends Product with Serializable

object Target {
  final case class Count(metric: CountMetric) extends Target
  final case class Percent(metric: CountMetric) extends Target
  final case class Time(metric: TimeMetric, stat: Stat) extends Target
  case object MeanRequestsPerSecond extends Target
}
sealed trait Condition extends Product with Serializable

object Condition {
  final case class Lte(value: Double) extends Condition
  final case class Gte(value: Double) extends Condition
  final case class Lt(value: Double) extends Condition
  final case class Gt(value: Double) extends Condition
  final case class Is(value: Double) extends Condition
  final case class Between(lowerBound: Double, upperBound: Double, inclusive: Boolean) extends Condition
  final case class In(elements: List[Double]) extends Condition
}
