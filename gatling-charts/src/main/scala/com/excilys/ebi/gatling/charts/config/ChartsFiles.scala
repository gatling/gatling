/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.config

import com.excilys.ebi.gatling.core.config.GatlingFiles._
import com.excilys.ebi.gatling.core.util.FileHelper._
import scala.tools.nsc.io.Path.string2path

object ChartsFiles {
	val JQUERY_FILE = "jquery.min.js"
	val MENU_FILE = "menu.js"

	val GATLING_TEMPLATE = "templates"
	val GATLING_TEMPLATE_STATISTICS_COMPONENT = GATLING_TEMPLATE / "statistics_component.html.ssp"
	val GATLING_TEMPLATE_LAYOUT_FILE = GATLING_TEMPLATE / "page_layout.html.ssp"
	val GATLING_TEMPLATE_MENU_JS_FILE = GATLING_TEMPLATE / "menu.js.ssp"

	def menuFile(runOn: String) = resultFolder(runOn) / GATLING_JS / MENU_FILE
	def activeSessionsFile(runOn: String) = resultFolder(runOn) / "active_sessions.html"
	def globalRequestsFile(runOn: String) = resultFolder(runOn) / "requests.html"

	def requestFile(runOn: String, requestName: String) = resultFolder(runOn) / (formatToFilename(requestName) + HTML_EXTENSION)
}