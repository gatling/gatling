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

package io.gatling.charts.template

import java.nio.charset.Charset

import io.gatling.charts.FileNamingConventions
import io.gatling.charts.component.Component
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.util.HtmlHelper._
import io.gatling.commons.shared.unstable.model.stats.Group
import io.gatling.commons.util.StringHelper._
import io.gatling.core.stats.writer.RunMessage

private[charts] object PageTemplate {

  private var runMessage: RunMessage = _
  private var runStart: Long = _
  private var runEnd: Long = _

  def setRunInfo(runMessage: RunMessage, runEnd: Long): Unit = {
    this.runMessage = runMessage
    this.runStart = runMessage.start
    this.runEnd = runEnd
  }
}

private[charts] abstract class PageTemplate(title: String, isDetails: Boolean, requestName: Option[String], group: Option[Group], components: Component*) {

  def jsFiles: Seq[String] = (CommonJsFiles ++ components.flatMap(_.jsFiles)).distinct

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def getOutput(charset: Charset): String = {
    val runMessage = PageTemplate.runMessage
    val runStart = PageTemplate.runStart
    val runEnd = PageTemplate.runEnd
    val duration = (runEnd - runStart) / 1000
    val simulationClassSimpleName = runMessage.simulationClassName.lastIndexOf(".") match {
      case -1 => runMessage.simulationClassName
      case i  => runMessage.simulationClassName.substring(i + 1)
    }

    val pageStats =
      if (isDetails) {
        val groupHierarchy = group.map(_.hierarchy).getOrElse(Nil).map(_.toGroupFileName(charset))

        val groupAndRequestHierarchy = requestName match {
          case Some(req) => groupHierarchy :+ req.toRequestFileName(charset)
          case _         => groupHierarchy
        }

        s"""var pageStats = stats.contents['${groupAndRequestHierarchy.mkString("'].contents['")}'].stats;"""
      } else {
        "var pageStats = stats.stats;"
      }

    s"""
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<link rel="shortcut icon" type="image/x-icon" href="style/favicon.ico"/>
<link href="style/style.css" rel="stylesheet" type="text/css" />
<link href="style/bootstrap.min.css" rel="stylesheet" type="text/css" />
${jsFiles.map(jsFile => s"""<script type="text/javascript" src="js/$jsFile"></script>""").mkString(Eol)}
<title>Gatling Stats - $title</title>
</head>
<body>
<div class="frise"></div>
<div class="head">
  <a class="logo" href="https://gatling.io" target="blank_" title="Gatling Home Page"><img alt="Gatling" src="style/logo.svg"/></a>
</div>
<div class="container details">
  <div class="nav">
    <ul></ul>
  </div>
  <div class="main">
    <div class="cadre">
      <div class="content">
        <div class="content-header">
          <div class="onglet">
            $simulationClassSimpleName
          </div>
          <div class="sous-menu" id="sousMenu">
            <div class="sous-menu-spacer">
              <div class="item ${if (!isDetails) "ouvert" else ""}"><a href="index.html">Global</a></div>
                <div class="item ${if (isDetails) "ouvert" else ""}"><a id="details_link" href="#">Details</a></div>
              </div>
              <script type="text/javascript">
                const runStartHumanDate = moment($runStart).format("YYYY-MM-DD HH:mm:ss Z");
                const runInfo = document.createElement('p');
                runInfo.setAttribute('class', 'sous-menu-spacer sim_desc');
                runInfo.style.align = 'right';
                runInfo.setAttribute('title', runStartHumanDate + ', duration : $duration seconds');
                runInfo.setAttribute('data-content', '${runMessage.runDescription.htmlEscape}');
                runInfo.innerHTML = "<b>" + runStartHumanDate + ", duration : $duration seconds ${runMessage.runDescription.truncate(70).htmlEscape}</b>";
                document.getElementById('sousMenu').appendChild(runInfo);
              </script>
            </div>
          </div>
          <div class="content-in">
            <div class="article">
              ${components.map(_.html).mkString}
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="enterprise">
      <img alt="Gatling Enterprise" src="style/logo-enterprise.svg" /><a href="https://gatling.io/enterprise/" target="_blank">Try Gatling Enterprise</a>
    </div>
</div>
<script type="text/javascript">
    $pageStats
    $$(document).ready(function() {
        $$('.sim_desc').popover({trigger:'hover', placement:'bottom'});
        setDetailsLinkUrl();
        ${if (isDetails) "setDetailsMenu();" else "setGlobalMenu();"}
        setActiveMenu();
        fillStats(pageStats);
        ${components.map(_.js).mkString}
    });
</script>
</body>
</html>
"""
  }
}
