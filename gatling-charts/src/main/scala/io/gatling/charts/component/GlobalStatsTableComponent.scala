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

package io.gatling.charts.component

import java.{ lang => jl }

import io.gatling.charts.report.{ Container, GroupContainer }
import io.gatling.charts.stats.Group
import io.gatling.charts.util.HtmlHelper.HtmlRichString
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.IndicatorsConfiguration
import io.gatling.shared.util.NumberHelper._

private[charts] final class GlobalStatsTableComponent(rootContainer: GroupContainer, configuration: IndicatorsConfiguration) extends Component {

  private val headContent = generateHtmlRow(rootContainer, 0, 0, None, group = false)
  private val (bodyContent, bodyIndex) = generateHtmlRowsForGroup(rootContainer, 0, 0, None)

  override def html: String = {
    def pctTitle(pct: Double) = pct.toRank + " pct"

    val responseTimeFields = List(
      "Min",
      pctTitle(configuration.percentile1),
      pctTitle(configuration.percentile2),
      pctTitle(configuration.percentile3),
      pctTitle(configuration.percentile4),
      "Max",
      "Mean",
      """<abbr title="Standard Deviation">Std Dev</abbr>"""
    )

    s"""
                      <div id="statistics_table_container">
                        <div id="stats" class="statistics extensible-geant collapsed">
                            <div class="title">
                              <div id="statistics_title" class="title_base"><span class="title_base_stats">Stats</span><span class="expand-table">Fixed height</span><span id="toggle-stats" class="toggle-table"></span><span class="collapse-table">Full size</span></div>
                              <div class="right">
                                  <button class="statistics-button expand-all-button">Expand all groups</button>
                                  <button class="statistics-button collapse-all-button">Collapse all groups</button>
                                  <button id="statistics_full_screen" class="statistics-button" onclick="openStatisticsTableModal()"><img alt="Fullscreen" src="style/fullscreen.svg"></button>
                              </div>
                            </div>
                            <div class="scrollable">
                              <table id="container_statistics_head" class="statistics-in extensible-geant">
                                  <thead>
                                      <tr>
                                          <th rowspan="2" id="col-1" class="header sortable sorted-up"><span>Requests</span></th>
                                          <th colspan="5" class="header"><span class="executions">Executions</span></th>
                                          <th colspan="${responseTimeFields.size}" class="header"><span class="response-time">Response Time (ms)</span></th>
                                      </tr>
                                      <tr>
                                          <th id="col-2" class="header sortable"><span>Total</span></th>
                                          <th id="col-3" class="header sortable"><span>OK</span></th>
                                          <th id="col-4" class="header sortable"><span>KO</span></th>
                                          <th id="col-5" class="header sortable"><span>% KO</span></th>
                                          <th id="col-6" class="header sortable"><span><abbr title="Count of events per second">Cnt/s</abbr></span></th>
                                          ${responseTimeFields.zipWithIndex
        .map { case (header, i) => s"""<th id="col-${i + 7}" class="header sortable"><span>$header</span></th>""" }
        .mkString(Eol)}
                                      </tr>
                                  </thead>
                                  <tbody>$headContent</tbody>
                              </table>
                              <table id="container_statistics_body" class="statistics-in extensible-geant">
                                  <tbody>$bodyContent</tbody>
                              </table>
                            </div>
                        </div>
                      </div>
<dialog id="statistics_table_modal" class="statistics-table-modal">
  <div class="statistics-table-modal-header"><button class="button-modal" onclick="closeStatisticsTableModal()"><img alt="Close" src="style/close.svg"></button></div>
  <div class="statistics-table-modal-container">
    <div id="statistics_table_modal_content" class="statistics-table-modal-content"></div>
  </div>
</dialog>
"""
  }

  private def generateHtmlRow(container: Container, level: Int, index: Int, parent: Option[String], group: Boolean): String = {
    val url = container match {
      case groupContainer: GroupContainer if groupContainer.group == Group.Root => "index.html"
      case _                                                                    => s"${container.id}.html"
    }

    val expandButtonStyle = container match {
      case _: GroupContainer => ""
      case _                 => "hidden"
    }

    val koPercent = container.stats.numberOfRequestsStatistics.total match {
      case Some(total) if total != 0 =>
        Some(container.stats.numberOfRequestsStatistics.failure.getOrElse(0L) * 100.0 / total)
      case _ =>
        None
    }

    val dataParent = parent.map(p => s"""data-parent="$p"""").getOrElse("")

    s"""<tr $dataParent>
       |  <td class="total col-1">
       |    <div class="expandable-container">
       |      <span id="${container.id}" style="margin-left: ${level * 10}px;" class="expand-button $expandButtonStyle">&nbsp;</span>
       |        <a href="$url" class="withTooltip">
       |          <span class="table-cell-tooltip" id="parent-stats-table-${container.id}" data-toggle="popover" data-placement="right" data-container="body" data-content="">
       |            <span onmouseover="isEllipsed('stats-table-${container.id}')" id="stats-table-${container.id}" class="ellipsed-name">${container.name.htmlEscape}</span>
       |          </span>
       |        </a>
       |      <span class="value" style="display:none;">$index</span>
       |    </div>
       |  </td>
       |  <td class="value total col-2">${style(container.stats.numberOfRequestsStatistics.total)}</td>
       |  <td class="value ok col-3">${style(container.stats.numberOfRequestsStatistics.success)}</td>
       |  <td class="value ko col-4">${style(container.stats.numberOfRequestsStatistics.failure)}</td>
       |  <td class="value ko col-5">${style(koPercent)}</td>
       |  <td class="value total col-6">${style(container.stats.meanNumberOfRequestsPerSecondStatistics.total)}</td>
       |  <td class="value total col-7">${style(container.stats.minResponseTimeStatistics.total)}</td>
       |  <td class="value total col-8">${style(container.stats.percentiles1.total)}</td>
       |  <td class="value total col-9">${style(container.stats.percentiles2.total)}</td>
       |  <td class="value total col-10">${style(container.stats.percentiles3.total)}</td>
       |  <td class="value total col-11">${style(container.stats.percentiles4.total)}</td>
       |  <td class="value total col-12">${style(container.stats.maxResponseTimeStatistics.total)}</td>
       |  <td class="value total col-13">${style(container.stats.meanResponseTimeStatistics.total)}</td>
       |  <td class="value total col-14">${style(container.stats.stdDeviationStatistics.total)}</td>
       |</tr>""".stripMargin
  }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def generateHtmlRowsForGroup(group: GroupContainer, level: Int, index: Int, parent: Option[String]): (String, Int) = {
    val buffer = new jl.StringBuilder
    var newIndex = index

    val newParent =
      if (parent.isEmpty) {
        Some(GroupContainer.RootId)
      } else {
        buffer.append(generateHtmlRow(group, level - 1, index, parent, group = true))
        newIndex += 1
        Some(group.id)
      }

    group.groups.values.foreach { group =>
      val (groupContent, groupIndex) = generateHtmlRowsForGroup(group, level + 1, index, newParent)
      buffer.append(groupContent)
      newIndex += groupIndex
    }

    group.requests.values.foreach { request =>
      buffer.append(generateHtmlRow(request, level, index, newParent, group = false))
      newIndex += 1
    }

    (buffer.toString, newIndex)
  }

  override def js = s"""
$$('#container_statistics_head').sortable('#container_statistics_body');
$$('.statistics').expandable();

${if (bodyIndex < 30) {
      "$('#statistics_title span').attr('style', 'display: none;');"
    } else {
      """$('#statistics_title').addClass('title_collapsed');
        |$('#statistics_title').click(function() {
        |    $('#toggle-stats').toggleClass("off");
        |    $(this).toggleClass('title_collapsed').toggleClass('title_expanded');
        |    $('#container_statistics_body').parent().toggleClass('scrollable').toggleClass('');
        |});""".stripMargin
    }}

$$('.table-cell-tooltip').popover({trigger:'hover'});
"""

  override def jsFiles: Seq[String] = "stats.js" :: Nil
}
