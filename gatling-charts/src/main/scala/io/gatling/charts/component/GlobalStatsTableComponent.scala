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

import io.gatling.charts.config.ChartsFiles.AllRequestLineTitle
import io.gatling.charts.report.Container.{ Group, Request }
import io.gatling.commons.util.NumberHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.config.GatlingConfiguration

private[charts] final class GlobalStatsTableComponent(implicit configuration: GatlingConfiguration) extends Component {

  private val MaxRequestNameSize = 22

  override val html: String = {

    def pctTitle(pct: Double) = pct.toRank + " pct"

    val pct1 = pctTitle(configuration.charting.indicators.percentile1)
    val pct2 = pctTitle(configuration.charting.indicators.percentile2)
    val pct3 = pctTitle(configuration.charting.indicators.percentile3)
    val pct4 = pctTitle(configuration.charting.indicators.percentile4)
    val responseTimeFields = Vector("Min", pct1, pct2, pct3, pct4, "Max", "Mean", """<abbr title="Standard Deviation">Std Dev</abbr>""")

    s"""
                        <div id="stats" class="statistics extensible-geant collapsed">
                            <div class="title">
                              <div id="statistics_title" class="title_base"><span class="title_base_stats">Stats</span><span class="expand-table">Fixed height</span><span id="toggle-stats" class="toggle-table"></span><span class="collapse-table">Full size</span></div>
                              <div class="right">
                                  <span class="expand-all-button">Expand all groups</span> <span class="collapse-all-button">Collapse all groups</span>
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
                                  <tbody></tbody>
                              </table>
                              <table id="container_statistics_body" class="statistics-in extensible-geant">
                                  <tbody></tbody>
                              </table>
                            </div>
                        </div>
"""
  }

  val js = s"""

  function shortenNameAndDisplayFullOnHover(name, level) {
  var maxNameSize = $MaxRequestNameSize - level * 2;
   if (name.length < maxNameSize) {
       return name;
   } else {
     var truncatedLength = Math.max(3, maxNameSize - 1);
     return "<span class='table-cell-tooltip' data-toggle='popover' data-placement='bottom' data-container='body' data-content='" + name + "'>"+name.substr(0,truncatedLength)+"&hellip;"+"</span>";
    }
  }

function generateHtmlRow(request, level, index, parent, group) {
    if (request.name == '$AllRequestLineTitle')
        var url = 'index.html';
    else
        var url = request.pathFormatted + '.html';

    if (group)
        var expandButtonStyle = '';
    else
        var expandButtonStyle = ' hidden';

    if (request.stats.numberOfRequests.total != 0)
        var koPercent = (request.stats.numberOfRequests.ko * 100 / request.stats.numberOfRequests.total).toFixed(0) + '%';
    else
        var koPercent = '-'

    return '<tr id="' + request.pathFormatted + '" data-parent=' + parent + '> \\
        <td class="total col-1"> \\
            <span id="' + request.pathFormatted + '" style="margin-left: ' + (level * 10) + 'px;" class="expand-button' + expandButtonStyle + '">&nbsp;</span> \\
            <a href="' + url +'" class="withTooltip">' + shortenNameAndDisplayFullOnHover(request.name, level) + '</a><span class="value" style="display:none;">' + index + '</span> \\
        </td> \\
        <td class="value total col-2">' + request.stats.numberOfRequests.total + '</td> \\
        <td class="value ok col-3">' + request.stats.numberOfRequests.ok + '</td> \\
        <td class="value ko col-4">' + request.stats.numberOfRequests.ko + '</td> \\
        <td class="value ko col-5">' + koPercent + '</td> \\
        <td class="value total col-6">' + request.stats.meanNumberOfRequestsPerSecond.total + '</td> \\
        <td class="value total col-7">' + request.stats.minResponseTime.total + '</td> \\
        <td class="value total col-8">' + request.stats.percentiles1.total + '</td> \\
        <td class="value total col-9">' + request.stats.percentiles2.total + '</td> \\
        <td class="value total col-10">' + request.stats.percentiles3.total + '</td> \\
        <td class="value total col-11">' + request.stats.percentiles4.total + '</td> \\
        <td class="value total col-12">' + request.stats.maxResponseTime.total + '</td> \\
        <td class="value total col-13">' + request.stats.meanResponseTime.total + '</td> \\
        <td class="value total col-14">' + request.stats.standardDeviation.total + '</td> \\
        </tr>';
}

function generateHtmlRowsForGroup(group, level, index, parent) {
    var buffer = '';

    if (!parent)
        parent = 'ROOT';
    else {
        buffer += generateHtmlRow(group, level - 1, index, parent, true);
        index++;
        parent = group.pathFormatted;
    }

    $$.each(group.contents, function(contentName, content) {
        if (content.type == '$Group') {
            var result = generateHtmlRowsForGroup(content, level + 1, index, parent);
            buffer += result.html;
            index = result.index;
        }
        else if (content.type == '$Request') {
            buffer += generateHtmlRow(content, level, index, parent);
            index++;
        }
    });

    return { html: buffer, index: index };
}

$$('#container_statistics_head tbody').append(generateHtmlRow(stats, 0, 0));

var lines = generateHtmlRowsForGroup(stats, 0, 0);
$$('#container_statistics_body tbody').append(lines.html);

$$('#container_statistics_head').sortable('#container_statistics_body');
$$('.statistics').expandable();

if (lines.index < 30) {
    $$('#statistics_title span').attr('style', 'display: none;');
} else {
    $$('#statistics_title').addClass('title_collapsed');
    $$('#statistics_title').click(function() {
        $$('#toggle-stats').toggleClass("off");
        $$(this).toggleClass('title_collapsed').toggleClass('title_expanded');
        $$('#container_statistics_body').parent().toggleClass('scrollable').toggleClass('');
    });
}
$$('.table-cell-tooltip').popover({trigger:'hover'});
"""

  val jsFiles: Seq[String] = Seq.empty
}