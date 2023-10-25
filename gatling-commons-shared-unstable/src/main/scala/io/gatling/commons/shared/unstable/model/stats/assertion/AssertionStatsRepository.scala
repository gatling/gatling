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

import io.gatling.commons.stats.Status

object AssertionStatsRepository {

  sealed trait StatsPath extends Product with Serializable

  object StatsPath {
    final case class Request(group: List[String], request: String) extends StatsPath

    final case class Group(group: List[String]) extends StatsPath
  }

  object Stats {
    val NoData: Stats = Stats(-1, -1, 0, -1, -1, _ => -1, -1)
  }

  final case class Stats(min: Int, max: Int, count: Long, mean: Int, stdDev: Int, percentile: Double => Int, meanRequestsPerSec: Double)
}

trait AssertionStatsRepository {

  def allRequestPaths(): List[AssertionStatsRepository.StatsPath.Request]

  def findPathByParts(parts: List[String]): Option[AssertionStatsRepository.StatsPath]

  def requestGeneralStats(group: List[String], request: Option[String], status: Option[Status]): AssertionStatsRepository.Stats
  def groupCumulatedResponseTimeGeneralStats(group: List[String], status: Option[Status]): AssertionStatsRepository.Stats
}
