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

object ChartsConfig {
	/**
	 * File name of the active sessions chart
	 */
	val GATLING_CHART_ACTIVE_SESSIONS_FILE = "active_sessions.html"
	/**
	 * File name of the requests chart
	 */
	val GATLING_CHART_GLOBAL_REQUESTS_FILE = "requests.html"
	/**
	 * File name of the javascript menu generator
	 */
	val GATLING_CHART_MENU_JS_FILE = "js/menu.js"
	/**
	 * File name of the request log file
	 */
	val GATLING_STATS_GLOBAL_REQUESTS_FILE = "requests.tsv"

	/**
	 * Path to the template for request details body
	 */
	val GATLING_CHARTS_STATISTICS_TEMPLATE = "templates/statistics_component.html.ssp"
	/**
	 * Path to the template for HTML reports
	 */
	val GATLING_TEMPLATE_LAYOUT_FILE = "templates/page_layout.html.ssp"
	/**
	 * Path to the template for the javascript menu generator
	 */
	val GATLING_TEMPLATE_MENU_JS_FILE = "templates/menu.js.ssp"
}