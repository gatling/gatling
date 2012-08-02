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

import scala.tools.nsc.io.{ Path, Directory }
import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

object GatlingFiles {
	val GATLING_HOME = Option(System.getenv("GATLING_HOME")).getOrElse(".")
	val GATLING_USER_FILES_FOLDER = GATLING_HOME / "user-files"
	val GATLING_ASSETS_PACKAGE = "assets"
	val GATLING_JS = "js"
	val GATLING_STYLE = "style"
	val GATLING_REQUEST_BODIES = "request-bodies"
	val GATLING_ASSETS_JS_PACKAGE = GATLING_ASSETS_PACKAGE / GATLING_JS
	val GATLING_ASSETS_STYLE_PACKAGE = GATLING_ASSETS_PACKAGE / GATLING_STYLE
	val GATLING_IMPORTS_FILE = "imports.txt"

	def dataDirectory: Path = configuration.dataDirectoryPath.getOrElse(GATLING_USER_FILES_FOLDER / "data")
	def resultsDirectory: Path = configuration.resultsDirectoryPath.getOrElse(GATLING_HOME / "results")
	def requestBodiesDirectory: Path = configuration.requestBodiesDirectoryPath.getOrElse(GATLING_USER_FILES_FOLDER / GATLING_REQUEST_BODIES)
	def simulationsDirectory: Path = configuration.simulationsDirectoryPath.getOrElse(GATLING_USER_FILES_FOLDER / "simulations")

	def resultDirectory(runUuid: String): Path = resultsDirectory / runUuid
	def jsDirectory(runUuid: String): Path = resultDirectory(runUuid) / GATLING_JS
	def styleDirectory(runUuid: String): Path = resultDirectory(runUuid) / GATLING_STYLE
	def rawDataDirectory(runUuid: String): Path = resultDirectory(runUuid) / "rawdata"
	def simulationLogDirectory(runUuid: String, create: Boolean = true): Directory = {
		val dir = resultDirectory(runUuid)
		if (create) dir.createDirectory()
		else if (!dir.exists) throw new IllegalArgumentException("simulation directory '" + dir + "' doesn't exist")
		else if (!dir.isDirectory) throw new IllegalArgumentException("simulation directory '" + dir + "' is not a directory")
		else dir.toDirectory
	}
}