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

package io.gatling.commons.shared.unstable.model.stats

import io.gatling.commons.stats.Status
import io.gatling.commons.stats.assertion.Assertion

trait GeneralStatsSource {

  def assertions: List[Assertion]
  def statsPaths: List[StatsPath]
  def requestGeneralStats(requestName: Option[String], group: Option[Group], status: Option[Status]): GeneralStats
  def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): GeneralStats
}
