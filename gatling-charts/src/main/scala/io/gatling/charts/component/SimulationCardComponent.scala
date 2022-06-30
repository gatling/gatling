/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.component

import java.time.{ Duration, Instant, ZoneId }
import java.time.format.DateTimeFormatter

import io.gatling.charts.stats.RunInfo
import io.gatling.charts.util.HtmlHelper._
import io.gatling.commons.util.GatlingVersion
import io.gatling.commons.util.StringHelper._

private[charts] final class SimulationCardComponent(runInfo: RunInfo) extends Component {

  override def html: String = {
    val printableRunDateTime = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC")).format(Instant.ofEpochMilli(runInfo.injectStart))
    val printableGatlingReleaseDate = GatlingVersion.ThisVersion.releaseDate.toLocalDate.toString
    val printableDuration = {
      val duration = Duration.ofMillis(runInfo.injectEnd - runInfo.injectStart)
      val days = duration.toDays
      val minusDays = duration.minusDays(days)
      val hours = minusDays.toHours
      val minusHours = minusDays.minusHours(hours)
      val minutes = minusHours.toMinutes
      val seconds = minusHours.minusMinutes(minutes).getSeconds

      (if (days > 0) s"${days}d " else "") +
        (if (hours > 0) s"${hours}h " else if (days > 0) "0h" else "") +
        (if (minutes > 0) s"${minutes}m " else if (days > 0 || hours > 0) "0m" else "") +
        (if (seconds > 0) s"${seconds}s " else "0s")
    }

    s"""<div class="simulation-card">
       |  <div class="simulation-version-information">
       |  <span class="simulation-information-title">Gatling Version</span>
       |    <span class="simulation-information-item">
       |      <span class="simulation-information-label">Version: </span>
       |      <span>${GatlingVersion.ThisVersion.fullVersion}</span>
       |    </span>
       |    <span class="simulation-information-item">
       |      <span class="simulation-information-label">Released: </span>
       |      <span>$printableGatlingReleaseDate</span>
       |    </span>
       |  </div>
       |  <div id="simulation-information" class="simulation-version-information">
       |    <span class="simulation-information-title">Run Information</span>
       |    <div class="simulation-information-container">
       |      <span class="simulation-information-item">
       |        <span class="simulation-information-label">Date: </span>
       |        <span>$printableRunDateTime GMT</span>
       |      </span>
       |      <span class="simulation-information-item">
       |        <span class="simulation-information-label">Duration: </span>
       |        <span>$printableDuration</span>
       |      </span>
       |      ${if (runInfo.runDescription.nonEmpty) {
      s"""<span class="simulation-tooltip simulation-information-item description" title="Description" data-content="${runInfo.runDescription.htmlEscape}">
       |        <span class="simulation-information-label">Description: </span>
       |        <span>${runInfo.runDescription.truncate(2300).htmlEscape}</span>
       |      </span>
       |""".stripMargin
    } else {
      """<span class="simulation-information-item">
        |        <span class="simulation-information-label">Description: </span>
        |        <span>&mdash;</span>
        |      </span>
        |""".stripMargin
    }}
       |    </div>
       |  </div>
       |</div>""".stripMargin
  }

  override def js: String = ""

  override def jsFiles: Seq[String] = Nil
}
