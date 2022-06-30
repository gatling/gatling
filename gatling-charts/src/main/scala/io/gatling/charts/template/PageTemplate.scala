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
import java.time.{ ZoneOffset, ZonedDateTime }

import io.gatling.charts.FileNamingConventions
import io.gatling.charts.component.Component
import io.gatling.charts.config.ChartsFiles._
import io.gatling.charts.stats.RunInfo
import io.gatling.commons.shared.unstable.model.stats.Group
import io.gatling.commons.util.GatlingVersion
import io.gatling.commons.util.StringHelper._

private[charts] abstract class PageTemplate(
    runInfo: RunInfo,
    title: String,
    isDetails: Boolean,
    requestName: Option[String],
    group: Option[Group],
    components: Component*
) {

  def jsFiles: Seq[String] = (CommonJsFiles ++ components.flatMap(_.jsFiles)).distinct

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def getOutput(charset: Charset): String = {
    val simulationClassSimpleName = runInfo.simulationClassName.lastIndexOf(".") match {
      case -1 => runInfo.simulationClassName
      case i  => runInfo.simulationClassName.substring(i + 1)
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

    val deprecationWarning = {
      val thisReleaseDate = GatlingVersion.ThisVersion.releaseDate
      val thisReleaseDatePlus1Year = thisReleaseDate.plusYears(1)

      val deprecationMessage = GatlingVersion.LatestRelease match {
        case Some(GatlingVersion(number, releaseDate)) if releaseDate.isAfter(thisReleaseDatePlus1Year) =>
          s"""
             |You are using Gatling ${GatlingVersion.ThisVersion.fullVersion}
             | released on ${thisReleaseDate.toLocalDate.toString}, more than 1 year ago.
             | Gatling $number is available since ${releaseDate.toLocalDate.toString}.
             |""".stripMargin
        case None if ZonedDateTime.now(ZoneOffset.UTC).isAfter(thisReleaseDatePlus1Year) =>
          s"""You are using Gatling ${GatlingVersion.ThisVersion.fullVersion}
             | released on ${thisReleaseDate.toLocalDate.toString}, more than 1 year ago.
             |""".stripMargin
        case _ =>
          ""
      }

      deprecationMessage.map(m => s"""<div class="alert-danger">
                                     |  $m.
                                     |  Please check the <a href="https://gatling.io/docs/gatling/reference/current/whats_new">new features</a>,
                                     |  the <a href="https://gatling.io/docs/gatling/reference/current/upgrading/">upgrade guides</a>,
                                     |  and consider upgrading.
                                     |</div>""".stripMargin)
    }

    s"""
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<link rel="shortcut icon" type="image/x-icon" href="style/favicon.ico"/>
<link href="style/style.css" rel="stylesheet" type="text/css" />
<link href="style/bootstrap.min.css" rel="stylesheet" type="text/css" />
${jsFiles.map(jsFile => s"""<script src="js/$jsFile"></script>""").mkString(Eol)}
<title>Gatling Stats - $title</title>
</head>
<body>
<div class="app-container">
<div class="frise"></div>
<div class="head">
  <div class="gatling-open-source">
    <a class="gatling-logo" href="https://gatling.io" target="blank_" title="Gatling Home Page"><img alt="Gatling" src="style/logo.svg"/></a>
    <a class="gatling-documentation" href="https://gatling.io/docs/" target="_blank">Documentation</a>
  </div>
  <a class="enterprise" href="https://gatling.io/enterprise/next-step/" target="_blank">Try <img alt="Gatling Enterprise" src="style/logo-enterprise.svg"/></a></div>
<div class="container details">
  <div class="nav">
    <ul></ul>
  </div>
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
    $pageStats
    $$(document).ready(function() {
        $$('.simulation-tooltip').popover({trigger:'hover', placement:'left'});
        setDetailsLinkUrl();
        ${if (isDetails) "setDetailsMenu();" else "setGlobalMenu();"}
        setActiveMenu();
        fillStats(pageStats);
        ${components.map(_.js).mkString}
    });
</script>
</div>
</body>
</html>
"""
  }
}
