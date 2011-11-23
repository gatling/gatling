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
	val GATLING_SEEDS_FOLDER = GATLING_USER_FILES_FOLDER + "/data"
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
	val GATLING_ASSETS_JS_FOLDER = GATLING_ASSETS_FOLDER + GATLING_JS
	val GATLING_STYLE = "/style"
	val GATLING_ASSETS_STYLE_FOLDER = GATLING_ASSETS_FOLDER + GATLING_STYLE
	/**
	 * JQuery file path relative to parent folder
	 */
	val GATLING_JQUERY = "/jquery.min.js"
	/**
	 * Highstocks file path relative to parent folder
	 */
	val GATLING_HIGHSTOCK = "/highstock.js"
	/**
	 * Highcharts file path relative to parent folder
	 */
	val GATLING_HIGHCHARTS = "/highcharts.js"
	/**
	 * Path to JQuery library in results
	 */
	val GATLING_JS_JQUERY = GATLING_JS + GATLING_JQUERY
	/**
	 * Path to Highstocks library in results
	 */
	val GATLING_JS_HIGHSTOCK = GATLING_JS + GATLING_HIGHSTOCK
	/**
	 * Path to Highcharts library in results
	 */
	val GATLING_JS_HIGHCHARTS = GATLING_JS + GATLING_HIGHCHARTS
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
	 * Path to Highstocks library in assets
	 */
	val GATLING_ASSETS_HIGHSTOCK = GATLING_ASSETS_FOLDER + GATLING_JS_HIGHSTOCK
	/**
	 * Path to Highcharts library in assets
	 */
	val GATLING_ASSETS_HIGHCHARTS = GATLING_ASSETS_FOLDER + GATLING_JS_HIGHCHARTS

}