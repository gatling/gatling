/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.config

import java.nio.file.Path

import io.gatling.commons.util.PathHelper._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.GatlingFiles._
import io.gatling.charts.FileNamingConventions

private[charts] object ChartsFiles {
  val JQueryFile = "jquery-3.4.1.min.js"
  val BootstrapFile = "bootstrap.min.js"
  val GatlingJsFile = "gatling.js"
  val MomentJsFile = "moment-2.2.40.min.js"
  val MenuFile = "menu.js"
  val AllSessionsFile = "all_sessions.js"
  val StatsJsFile = "stats.js"
  val StatsJsonFile = "stats.json"
  val GlobalStatsJsonFile = "global_stats.json"
  val AssertionsJsonFile = "assertions.json"
  val AssertionsJUnitFile = "assertions.xml"
  val GlobalPageName = "Global Information"

  val CommonJsFiles = Seq(
    JQueryFile,
    BootstrapFile,
    GatlingJsFile,
    MomentJsFile,
    MenuFile,
    AllSessionsFile,
    StatsJsFile
  )

  def menuFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / MenuFile

  def allSessionsFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / AllSessionsFile

  def globalFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = resultDirectory(runUuid) / "index.html"

  def requestFile(runUuid: String, requestName: String)(implicit configuration: GatlingConfiguration): Path =
    resultDirectory(runUuid) / (requestName.toRequestFileName(configuration.core.charset) + ".html")

  def groupFile(runUuid: String, requestName: String)(implicit configuration: GatlingConfiguration): Path =
    resultDirectory(runUuid) / (requestName.toGroupFileName(configuration.core.charset) + ".html")

  def statsJsFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / StatsJsFile

  def statsJsonFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / StatsJsonFile

  def globalStatsJsonFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / GlobalStatsJsonFile

  def assertionsJsonFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / AssertionsJsonFile

  def assertionsJUnitFile(runUuid: String)(implicit configuration: GatlingConfiguration): Path = jsDirectory(runUuid) / AssertionsJUnitFile
}
