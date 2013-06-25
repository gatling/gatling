/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.component

import com.dongxiguo.fastring.Fastring.Implicits.{ FastringContext, MkFastring }

import io.gatling.charts.config.ChartsFiles.GLOBAL_PAGE_NAME
import io.gatling.charts.report.Container.{ GROUP, REQUEST }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.NumberHelper.formatNumberWithSuffix
import io.gatling.core.util.StringHelper.eol

class StatisticsTableComponent extends Component {

	val html = {

		val pct1 = formatNumberWithSuffix(configuration.charting.indicators.percentile1) + " pct"
		val pct2 = formatNumberWithSuffix(configuration.charting.indicators.percentile2) + " pct"
		val responseTimeFields: List[String] = List("Min", "Max", "Mean", "Std Dev", pct1, pct2, "Req/s")

		fast"""
                        <div class="statistics extensible-geant collapsed">
                            <div class="title">
                                <div class="right">
                                    <span class="expand-all-button">Expand all groups</span> | <span class="collapse-all-button">Collapse all groups</span>
                                </div>
                                <div id="statistics_title" class="title_collapsed">STATISTICS <span>(Click here to show more)</span></div>
                            </div>
                            <table id="container_statistics_head" class="statistics-in extensible-geant">
                                <thead>
                                    <tr>
                                        <th rowspan="2" id="col-1" class="header sortable sorted-up"><span>Requests</span></th>
                                        <th rowspan="2"></th>
                                        <th colspan="4" class="header"><span class="executions">Executions</span></th>
                                        <th rowspan="2"></th>
                                        <th colspan="${responseTimeFields.size}" class="header"><span class="response-time">Response Time (ms)</span></th>
                                    </tr>
                                    <tr>
                                        <th id="col-2" class="header sortable"><span>Total</span></th>
                                        <th id="col-3" class="header sortable"><span>OK</span></th>
                                        <th id="col-4" class="header sortable"><span>KO</span></th>
                                        <th id="col-5" class="header sortable"><span>% KO</span></th>
                                        ${responseTimeFields.zipWithIndex.map { case (header, i) => fast"""<th id="col-${i + 6}" class="header sortable"><span>$header</span></th>""" }.mkFastring(eol)}
                                    </tr>
                                </thead>
                                <tbody></tbody>
                            </table>
                            <div class="scrollable">
                                <table id="container_statistics_body" class="statistics-in extensible-geant">
                                    <tbody></tbody>
                                </table>
                            </div>
                        </div>
"""
	}

	val js = fast"""
function generateHtmlRow(request, level, index, parent, group) {
    if (request.name == '$GLOBAL_PAGE_NAME')
        var url = 'index.html';
    else
        var url ='req_' + request.pathFormatted + '.html';

    if (group)
        var expandButtonStyle = '';
    else
        var expandButtonStyle = ' hidden';

    var koPercent = (request.stats.numberOfRequests.ko * 100 / request.stats.numberOfRequests.total).toFixed(0);
    return '<tr id="' + request.pathFormatted + '" class="child-of-' + parent + '"> \\
        <td class="total col-1"> \\
            <span id="' + request.pathFormatted + '" style="margin-left: ' + (level * 10) + 'px;" class="expand-button' + expandButtonStyle + '">&nbsp;</span> \\
            <a href="' + url +'">' + request.name + '</a><span class="value" style="display:none;">' + index + '</span> \\
        </td> \\
        <td></td> \\
        <td class="value total col-2">' + request.stats.numberOfRequests.total + '</td> \\
        <td class="value ok col-3">' + request.stats.numberOfRequests.ok + '</td> \\
        <td class="value ko col-4">' + request.stats.numberOfRequests.ko + '</td> \\
         <td class="value ko col-5">' + koPercent + ' %' + '</td> \\
        <td></td> \\
        <td class="value total col-6">' + request.stats.minResponseTime.total + '</td> \\
        <td class="value total col-7">' + request.stats.maxResponseTime.total + '</td> \\
        <td class="value total col-8">' + request.stats.meanResponseTime.total + '</td> \\
        <td class="value total col-9">' + request.stats.standardDeviation.total + '</td> \\
        <td class="value total col-10">' + request.stats.percentiles1.total + '</td> \\
        <td class="value total col-11">' + request.stats.percentiles2.total + '</td> \\
        <td class="value total col-12">' + request.stats.meanNumberOfRequestsPerSecond.total + '</td> \\
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
        if (content.type == '$GROUP') {
            var result = generateHtmlRowsForGroup(content, level + 1, index, parent);
            buffer += result.html;
            index = result.index;
        }
        else if (content.type == '$REQUEST') {
            buffer += generateHtmlRow(content, level, index, parent);
            index++;
        }
    });

    return { html: buffer, index: index };
}

$$('#container_statistics_head tbody').append(generateHtmlRow(stats, 0, 0));

var lines = generateHtmlRowsForGroup(stats, 0, 0)
$$('#container_statistics_body tbody').append(lines.html);

$$('#container_statistics_head').sortable('#container_statistics_body');
$$('.statistics').expandable();

if (lines.index < 30) {
    $$('#statistics_title span').attr('style', 'display: none;');
    $$('#statistics_title').attr('style', 'cursor: auto;')
}
else {
    $$('#statistics_title').click(function(){
        $$(this).toggleClass('title_collapsed').toggleClass('title_not_collapsed');
        $$('#container_statistics_body').parent().toggleClass('scrollable').toggleClass('');
    });
}
"""

	val jsFiles: Seq[String] = Seq.empty
}
