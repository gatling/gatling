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

import java.time.{ ZoneOffset, ZonedDateTime }

import io.gatling.charts.component.Component
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.report.GroupContainer
import io.gatling.charts.stats.RunInfo
import io.gatling.charts.util.HtmlHelper.HtmlRichString
import io.gatling.commons.util.GatlingVersion
import io.gatling.commons.util.StringHelper._

private[charts] abstract class PageTemplate(
    runInfo: RunInfo,
    title: String,
    rootContainer: GroupContainer,
    components: Component*
) {
  def jsFiles: Seq[String] = (CommonJsFiles ++ components.flatMap(_.jsFiles)).distinct

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def getOutput: String = {
    val simulationClassSimpleName = runInfo.simulationClassName.lastIndexOf(".") match {
      case -1 => runInfo.simulationClassName
      case i  => runInfo.simulationClassName.substring(i + 1)
    }

    val deprecationWarning = {
      val thisReleaseDate = GatlingVersion.ThisVersion.releaseDate
      val thisReleaseDatePlus1Year = thisReleaseDate.plusYears(1)

      val deprecationMessage = GatlingVersion.LatestRelease match {
        case Some(GatlingVersion(number, releaseDate)) if releaseDate.isAfter(thisReleaseDatePlus1Year) =>
          Some(s"""
                  |You are using Gatling ${GatlingVersion.ThisVersion.fullVersion}
                  | released on ${thisReleaseDate.toLocalDate.toString}, more than 1 year ago.
                  | Gatling $number is available since ${releaseDate.toLocalDate.toString}.
                  |""".stripMargin)
        case None if ZonedDateTime.now(ZoneOffset.UTC).isAfter(thisReleaseDatePlus1Year) =>
          Some(s"""You are using Gatling ${GatlingVersion.ThisVersion.fullVersion}
                  | released on ${thisReleaseDate.toLocalDate.toString}, more than 1 year ago.
                  |""".stripMargin)
        case _ =>
          None
      }

      deprecationMessage
        .map(m => s"""<div class="alert-danger">
                     |  $m
                     |  Please check the <a href="https://gatling.io/docs/gatling/reference/current/whats_new">new features</a>,
                     |  the <a href="https://gatling.io/docs/gatling/reference/current/upgrading/">upgrade guides</a>,
                     |  and consider upgrading.
                     |</div>""".stripMargin)
        .getOrElse("")
    }

    s"""
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<link rel="shortcut icon" type="image/x-icon" href="style/favicon.ico">
<link href="style/style.css" rel="stylesheet" type="text/css">
<link href="style/bootstrap.min.css" rel="stylesheet" type="text/css">
${jsFiles.map(jsFile => s"""<script src="js/$jsFile"></script>""").mkString(Eol)}
<title>Gatling Stats - ${title.htmlEscape}</title>
</head>
<body>
<script>
  const storedTheme = localStorage.getItem('theme') || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  if (storedTheme) document.documentElement.setAttribute('data-theme', storedTheme)

  function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute("data-theme");
    const targetTheme = currentTheme === "light" ? "dark" : "light";

    document.documentElement.setAttribute('data-theme', targetTheme)
    localStorage.setItem('theme', targetTheme);
  };
</script>
<div class="app-container">
<div class="frise"></div>
<div class="head">
  <div class="gatling-open-source">
    <a class="gatling-logo gatling-logo-light" href="https://gatling.io" target="blank_" title="Gatling Home Page"><img alt="Gatling" src="style/logo-light.svg"></a>
    <a class="gatling-logo gatling-logo-dark" href="https://gatling.io" target="blank_" title="Gatling Home Page"><img alt="Gatling" src="style/logo-dark.svg"></a>
    <a class="gatling-documentation" href="https://gatling.io/docs/" target="_blank">Documentation</a>
  </div>
  <div class="nav spacer"></div>
  <a class="enterprise" href="https://gatling.io/enterprise/next-step/" target="_blank">
    <span class="button-text-state">Try Enterprise Edition</span>
    <span class="button-text-hover">Try Enterprise Edition</span>
  </a>
  <button id="theme-toggle" class="theme-toggle" type="button" onclick="toggleTheme()" aria-label="Toggle user interface mode">
    <span class="toggle-dark"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-moon"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg></span>
    <span class="toggle-light"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="feather feather-sun"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg></span>
  </button>
</div>
<div class="container details">
  <div class="nav">
    <ul>$getMenu</ul>
  </div>
  <div class="cadre">
    <div class="content">
      <div class="content-header">
        <div class="onglet">
          $simulationClassSimpleName
        </div>
        <div class="sous-menu" id="sousMenu">
          <div class="sous-menu-spacer">
            $getSubMenu
          </div>
        </div>
      </div>
      <div class="content-in">
        <div class="container-article">
          <div class="article">
            $deprecationWarning
            ${components.map(_.html).mkString}
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<script>
    $$(document).ready(function() {
        $$('.simulation-tooltip').popover({trigger:'hover', placement:'left'});
        $onDocumentReady
        setActiveMenu();
        ${components.map(_.js).mkString}
    });
</script>
</div>
</body>
</html>
"""
  }

  protected def getSubMenu: String

  protected def getMenu: String

  protected def getFirstDetailPageUrl: String =
    rootContainer.groups.values.headOption.getOrElse(rootContainer.requests.values.head).id + ".html"

  protected def onDocumentReady: String
}
