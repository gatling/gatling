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
package io.gatling.charts.template

import io.gatling.BaseSpec
import io.gatling.core.config.GatlingConfiguration
import io.gatling.charts.component.Statistics
import io.gatling.charts.component.GroupedCount

class ConsoleTemplateSpec extends BaseSpec {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "console template" should "format the request counters properly" in {
    val numberOfRequestsStatistics = Statistics("numberOfRequestsStatistics", 20l, 19l, 1l)
    val out = ConsoleTemplate.writeRequestCounters(numberOfRequestsStatistics)
    out.mkString shouldBe "> numberOfRequestsStatistics                            20 (OK=19     KO=1     )"
  }

  it should "format the grouped counts properly" in {
    val out = ConsoleTemplate.writeGroupedCounters(GroupedCount("t < 42 ms", 90, (90 / 0.42d).round.toInt))
    out.mkString shouldBe "> t < 42 ms                                             90 ( 42%)"
  }
}
