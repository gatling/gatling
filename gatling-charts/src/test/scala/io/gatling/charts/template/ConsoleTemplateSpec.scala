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
package io.gatling.charts.template

import scala.collection.mutable.Map
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import io.gatling.core.config.GatlingConfiguration
import io.gatling.charts.component.Statistics
import io.gatling.charts.component.RequestStatistics
import io.gatling.charts.component.GroupedCount

@RunWith(classOf[JUnitRunner])
class ConsoleTemplateSpec extends Specification {

  GatlingConfiguration.setUp()

  "console template" should {

    "format the request counters properly" in {
      val numberOfRequestsStatistics = Statistics("numberOfRequestsStatistics", 20l, 19l, 1l)
      val out = ConsoleTemplate.writeRequestCounters(numberOfRequestsStatistics)
      out.mkString must beEqualTo("> numberOfRequestsStatistics                            20 (OK=19     KO=1     )")
    }

    "format the grouped counts properly" in {
      val out = ConsoleTemplate.writeGroupedCounters(GroupedCount("t < 42 ms", 90, 42))
      out.mkString must beEqualTo("> t < 42 ms                                             90 ( 42%)")
    }

  }
}
