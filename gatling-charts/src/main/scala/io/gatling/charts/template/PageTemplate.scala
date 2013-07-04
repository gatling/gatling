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
package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.charts.component.Component
import io.gatling.charts.config.ChartsFiles.{ ALL_SESSIONS_FILE, BOOTSTRAP_FILE, GATLING_JS_FILE, JQUERY_FILE, MENU_FILE, STATS_JS_FILE }
import io.gatling.core.result.Group
import io.gatling.core.result.message.RunMessage
import io.gatling.core.util.DateHelper.RichDateTime
import io.gatling.core.util.FileHelper.FileRichString
import io.gatling.core.util.HtmlHelper.HtmlRichString
import io.gatling.core.util.StringHelper.{ eol, RichString }

object PageTemplate {

	private var runMessage: RunMessage = _
	private var runStart: Long = _
	private var runEnd: Long = _

	def setRunInfo(runMessage: RunMessage, runStart: Long, runEnd: Long) {
		this.runMessage = runMessage
		this.runStart = runStart
		this.runEnd = runEnd
	}
}

abstract class PageTemplate(title: String, isDetails: Boolean, requestName: Option[String], group: Option[Group], components: Component*) {

	def jsFiles: Seq[String] = (Seq(JQUERY_FILE, BOOTSTRAP_FILE, GATLING_JS_FILE, MENU_FILE, ALL_SESSIONS_FILE, STATS_JS_FILE) ++ components.flatMap(_.jsFiles)).distinct

	def getOutput: Fastring = {
		val runMessage = PageTemplate.runMessage
		val runStart = PageTemplate.runStart
		val runEnd = PageTemplate.runEnd
		val duration = (runEnd - runStart) / 1000

		val pageStats =
			if (isDetails) {
				val groupHierarchy = group.map(_.hierarchy).getOrElse(Nil)
				val req = requestName.map(List(_)).getOrElse(Nil)
				s"""var pageStats = stats.contents['${(groupHierarchy ::: req).map(_.toFileName).mkString("'].contents['")}'].stats;"""
			} else {
				"var pageStats = stats.stats;"
			}

		fast"""
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="shortcut icon" type="image/x-icon" href="style/favicon.ico"/>
<link href="style/style.css" rel="stylesheet" type="text/css" />
<link href="style/bootstrap.min.css" rel="stylesheet" type="text/css" />
<title>Gatling Stats - $title</title>
</head>
<body>
<div class="frise"></div>
<div class="container details">
    <div class="head">
        <a href="http://gatling-tool.org" target="blank_" title="Gatling Home Page"><img alt="Gatling" src="style/logo.png"/></a>
    </div>
    <div class="main">
        <div class="cadre">
                <div class="onglet">
                    <img src="style/cible.png" />
                    <p><span>${runMessage.simulationId}</span></p>
                </div>
                <div class="content">
                    <div class="sous-menu">
                        <div class="item ${if (!isDetails) "ouvert" else ""}"><a href="index.html">GLOBAL</a></div>
                        <div class="item ${if (isDetails) "ouvert" else ""}"><a id="details_link" href="#">DETAILS</a></div>
                        <p class="sim_desc" title="${runMessage.runDate.toHumanDate}, duration : $duration seconds" data-content="${runMessage.runDescription.htmlEscape}">
                            <b>${runMessage.runDate.toHumanDate}, duration : $duration seconds</b> ${runMessage.runDescription.truncate(70).htmlEscape}</b>
                        </p>
                    </div>
                    <div class="content-in">
                        <h1><span>> </span>$title</h1>
                        <div class="article">
                            ${components.map(_.html).mkFastring}
                        </div>
                    </div>
                </div>
        </div>
    </div>
    <div class="nav">
        <ul></ul>
    </div>
</div>
<div class="foot">
    <a href="http://gatling-tool.org" title="Gatling Home Page"><img alt="Gatling" src="style/logo-gatling.jpg"/></a>
</div>
${jsFiles.map(jsFile => fast"""<script type="text/javascript" src="js/$jsFile"></script>""").mkFastring(eol)}
<script type="text/javascript">
    $pageStats
    $$(document).ready(function() {
        $$('.sim_desc').popover({trigger:'hover', placement:'bottom'});
        setDetailsLinkUrl();
        ${if (isDetails) "setDetailsMenu();" else "setGlobalMenu();"}
        setActiveMenu();
        fillStats(pageStats);
        ${components.map(_.js).mkFastring}
    });
</script>
</body>
</html>
"""
	}
}
