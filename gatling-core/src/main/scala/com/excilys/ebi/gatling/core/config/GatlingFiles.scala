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
package com.excilys.ebi.gatling.core.config

import scala.tools.nsc.io.Path.string2path

object GatlingFiles {

	/**
	 * the root folder of gatling application
	 */
	val GATLING_HOME = System.getenv("GATLING_HOME")

	/**
	 * Gatling's configuration folder
	 */
	val GATLING_CONFIG_FOLDER = GATLING_HOME / "conf"

	/**
	 * Name of the default configuration file
	 */
	val GATLING_CONFIG_FILE = "gatling.conf"

	/**
	 * Gatling's user files folder
	 */
	val GATLING_USER_FILES_FOLDER = GATLING_HOME / "user-files"

	/**
	 * Gatling's data folder
	 */
	val GATLING_DATA_FOLDER = GATLING_USER_FILES_FOLDER / "data"

	/**
	 * Gatling's request bodies folder
	 */
	val GATLING_REQUEST_BODIES_FOLDER = GATLING_USER_FILES_FOLDER / "request-bodies"

	/**
	 * Gatling's templates folder
	 */
	val GATLING_TEMPLATES_FOLDER = GATLING_USER_FILES_FOLDER / "templates"

	/**
	 * Gatling's results folder
	 */
	val GATLING_RESULTS_FOLDER = GATLING_HOME / "results"

	/**
	 * Gatling's assets folder
	 */
	val GATLING_ASSETS_FOLDER = GATLING_HOME / "assets"

	val GATLING_JS = "js"

	val GATLING_ASSETS_JS_FOLDER = GATLING_ASSETS_FOLDER / GATLING_JS

	val GATLING_STYLE = "style"

	val GATLING_ASSETS_STYLE_FOLDER = GATLING_ASSETS_FOLDER / GATLING_STYLE

	/**
	 * Path to raw results
	 */
	val GATLING_RAWDATA_FOLDER = "rawdata"

	/**
	 * Gatling's scenarios folder
	 */
	val GATLING_SCENARIOS_FOLDER = GATLING_USER_FILES_FOLDER / "scenarios"

	/**
	 * Name of the simulation log
	 */
	val GATLING_SIMULATION_LOG_FILE = "simulation.log"
}