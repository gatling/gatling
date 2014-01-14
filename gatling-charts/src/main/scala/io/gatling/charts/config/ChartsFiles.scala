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

import io.gatling.core.config.GatlingFiles.{ GATLING_JS, resultDirectory }
import io.gatling.core.util.FileHelper.FileRichString

object ChartsFiles {
	val JQUERY_FILE = "jquery.min.js"
	val BOOTSTRAP_FILE = "bootstrap.min.js"
	val GATLING_JS_FILE = "gatling.js"
	val MENU_FILE = "menu.js"
	val ALL_SESSIONS_FILE = "all_sessions.js"
	val STATS_JS_FILE = "stats.js"
	val STATS_JSON_FILE = "global_stats.json"
	val STATS_TSV_FILE = "stats.tsv"
	val GLOBAL_PAGE_NAME = "Global Information"

	def menuFile(runOn: String): Path = resultDirectory(runOn) / GATLING_JS / MENU_FILE

	def allSessionsFile(runOn: String): Path = resultDirectory(runOn) / GATLING_JS / ALL_SESSIONS_FILE

	def globalFile(runOn: String): Path = resultDirectory(runOn) / "index.html"

	def requestFile(runOn: String, requestName: String): Path = resultDirectory(runOn) / requestName.toRequestFileName

	def jsStatsFile(runOn: String): Path = resultDirectory(runOn) / GATLING_JS / STATS_JS_FILE

	def jsonStatsFile(runOn: String): Path = resultDirectory(runOn) / GATLING_JS / STATS_JSON_FILE

	def tsvStatsFile(runOn: String): Path = resultDirectory(runOn) / STATS_TSV_FILE
}