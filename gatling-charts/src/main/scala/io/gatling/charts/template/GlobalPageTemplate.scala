/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.charts.component.Component
import io.gatling.charts.report.{ GlobalReportGenerator, GroupContainer }
import io.gatling.charts.stats.RunInfo

private[charts] final class GlobalPageTemplate(runInfo: RunInfo, rootContainer: GroupContainer, components: Component*)
    extends PageTemplate(runInfo, "Global Information", rootContainer, components: _*) {
  override protected def getSubMenu: String =
    s"""<div class="item ouvert"><a href="index.html">Global</a></div>
       |<div class="item"><a id="details_link" href="$getFirstDetailPageUrl">Details</a></div>""".stripMargin

  override protected def getMenu: String =
    s"""<li><a class="item" href="#${GlobalReportGenerator.RangesContainerId}"><span class="nav-label">Ranges</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.StatsContainerId}"><span class="nav-label">Stats</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.UserStartRateContainerId}"><span class="nav-label">Users start rate</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.MaxConcurrentUsersContainerId}"><span class="nav-label">Concurrent users</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.ResponseTimeDistributionContainerId}"><span class="nav-label">Response time distribution</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.ResponseTimeContainerId}"><span class="nav-label">Response time percentiles</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.RequestsContainerId}"><span class="nav-label">Requests / sec</span></a></li>
       |<li><a class="item" href="#${GlobalReportGenerator.ResponsesContainerId}"><span class="nav-label">Responses / sec</span></a></li>""".stripMargin

  override protected def onDocumentReady: String = ""
}
