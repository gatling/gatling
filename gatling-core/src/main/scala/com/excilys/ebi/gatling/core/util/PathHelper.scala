/*
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
package com.excilys.ebi.gatling.core.util

/**
 * This object groups all utilities for paths
 */
object PathHelper {
	/**
	 * the root folder of gatling application
	 */
	val GATLING_HOME = System.getenv("GATLING_HOME")

	/**
	 * Gatling's configuration folder
	 */
	val GATLING_CONFIG_FOLDER = GATLING_HOME + "/conf"
	/**
	 * Gatling's results folder
	 */
	val GATLING_RESULTS_FOLDER = GATLING_HOME + "/results"
	/**
	 * Gatling's user files folder
	 */
	val GATLING_USER_FILES_FOLDER = GATLING_HOME + "/user-files"
	/**
	 * Gatling's assets folder
	 */
	val GATLING_ASSETS_FOLDER = GATLING_HOME + "/assets"

	/**
	 * Gatling's seeds folder
	 */
	val GATLING_SEEDS_FOLDER = GATLING_USER_FILES_FOLDER + "/seeds"
	/**
	 * Gatling's scenarios folder
	 */
	val GATLING_SCENARIOS_FOLDER = GATLING_USER_FILES_FOLDER + "/scenarios"
	/**
	 * Gatling's request bodies folder
	 */
	val GATLING_REQUEST_BODIES_FOLDER = GATLING_USER_FILES_FOLDER + "/request-bodies"
	/**
	 * Gatling's templates folder
	 */
	val GATLING_TEMPLATES_FOLDER = GATLING_USER_FILES_FOLDER + "/templates"
	/**
	 * Directory where javascript files are stored in results
	 */
	val GATLING_JS = "/js"
	val GATLING_JQUERY = "/jquery.min.js"
	val GATLING_HIGHCHARTS = "/highcharts.js"
	/**
	 * Path to JQuery library in results
	 */
	val GATLING_JS_JQUERY = "/js" + GATLING_JQUERY
	/**
	 * Path to Highcharts library in results
	 */
	val GATLING_JS_HIGHCHARTS = "/js" + GATLING_HIGHCHARTS
	/**
	 * Path to raw results
	 */
	val GATLING_RAWDATA_FOLDER = "/rawdata"
	/**
	 * Name of the simulation log
	 */
	val GATLING_SIMULATION_LOG_FILE = "simulation.log"
	/**
	 * Name of the default configuration file
	 */
	val GATLING_CONFIG_FILE = "gatling.conf"
	/**
	 * Path to JQuery library in assets
	 */
	val GATLING_ASSETS_JQUERY = GATLING_ASSETS_FOLDER + GATLING_JS_JQUERY
	/**
	 * Path to Highcharts library in assets
	 */
	val GATLING_ASSETS_HIGHCHARTS = GATLING_ASSETS_FOLDER + GATLING_JS_HIGHCHARTS

	/**
	 * File name of the active sessions graph
	 */
	val GATLING_GRAPH_ACTIVE_SESSIONS_FILE = "active_sessions.html"
	/**
	 * File name of the requests graph
	 */
	val GATLING_GRAPH_GLOBAL_REQUESTS_FILE = "requests.html"
	/**
	 * File name of the javascript menu generator
	 */
	val GATLING_GRAPH_MENU_JS_FILE = "js/menu.js"
	/**
	 * File name of the request log file
	 */
	val GATLING_STATS_GLOBAL_REQUESTS_FILE = "requests.tsv"

	/**
	 * Path to the template for request details body
	 */
	val GATLING_TEMPLATE_REQUEST_DETAILS_BODY_FILE = "templates/details_requests_body.ssp"
	/**
	 * Path to the template for highcharts column graphs
	 */
	val GATLING_TEMPLATE_HIGHCHARTS_COLUMN_FILE = "templates/highcharts_column.ssp"
	/**
	 * Path to the template for highcharts time graphs
	 */
	val GATLING_TEMPLATE_HIGHCHARTS_TIME_FILE = "templates/highcharts_time.ssp"
	/**
	 * Path to the template for HTML reports
	 */
	val GATLING_TEMPLATE_LAYOUT_FILE = "templates/layout.ssp"
	/**
	 * Path to the template for the javascript menu generator
	 */
	val GATLING_TEMPLATE_MENU_JS_FILE = "templates/menu_js.ssp"

}