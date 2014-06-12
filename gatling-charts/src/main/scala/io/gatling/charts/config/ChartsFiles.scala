/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.config

import scala.tools.nsc.io.Path
import scala.tools.nsc.io.Path.string2path

import io.gatling.core.config.GatlingFiles._
import io.gatling.charts.FileNamingConventions

object ChartsFiles {
  val JQueryFile = "jquery.min.js"
  val BootstrapFile = "bootstrap.min.js"
  val GatlingJsFile = "gatling.js"
  val MenuFile = "menu.js"
  val AllSessionsFile = "all_sessions.js"
  val StatsJsFile = "stats.js"
  val StatsJSONFile = "global_stats.json"
  val GlobalPageName = "Global Information"

  def menuFile(runOn: String): Path = resultDirectory(runOn) / GatlingJsFolder / MenuFile

  def allSessionsFile(runOn: String): Path = resultDirectory(runOn) / GatlingJsFolder / AllSessionsFile

  def globalFile(runOn: String): Path = resultDirectory(runOn) / "index.html"

  def requestFile(runOn: String, requestName: String): Path = resultDirectory(runOn) / requestName.toRequestFileName

  def jsStatsFile(runOn: String): Path = resultDirectory(runOn) / GatlingJsFolder / StatsJsFile

  def jsonStatsFile(runOn: String): Path = resultDirectory(runOn) / GatlingJsFolder / StatsJSONFile
}
