/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.config

import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

object GatlingFiles {
	val GATLING_HOME = Option(System.getenv("GATLING_HOME")).getOrElse(System.getProperty("java.io.tmpdir"))
	val GATLING_USER_FILES_FOLDER = GATLING_HOME / "user-files"
	val GATLING_ASSETS_PACKAGE = "assets"
	val GATLING_JS = "js"
	val GATLING_STYLE = "style"
	val GATLING_ASSETS_JS_PACKAGE = GATLING_ASSETS_PACKAGE + "/" + GATLING_JS
	val GATLING_ASSETS_STYLE_PACKAGE = GATLING_ASSETS_PACKAGE + "/" + GATLING_STYLE
	val GATLING_IMPORTS_FILE = "imports.txt"

	def dataFolder = configuration.dataFolderPath.getOrElse(GATLING_USER_FILES_FOLDER / "data")
	def resultsFolder = configuration.resultsFolderPath.getOrElse(GATLING_HOME / "results")
	def requestBodiesFolder = configuration.requestBodiesFolderPath.getOrElse(GATLING_USER_FILES_FOLDER / "request-bodies")
	def simulationsFolder = configuration.simulationsFolderPath.getOrElse(GATLING_USER_FILES_FOLDER / "simulations")

	def resultFolder(runOn: String) = resultsFolder / runOn
	def jsFolder(runOn: String) = resultFolder(runOn) / GATLING_JS
	def styleFolder(runOn: String) = resultFolder(runOn) / GATLING_STYLE
	def rawdataFolder(runOn: String) = resultFolder(runOn) / "rawdata"
	def simulationLogFile(runOn: String) = resultFolder(runOn) / "simulation.log"
}