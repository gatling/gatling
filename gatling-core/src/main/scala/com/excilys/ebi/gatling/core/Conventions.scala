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
package com.excilys.ebi.gatling.core

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.FileHelper._

object Conventions extends Logging {

	/**
	 * Separator used in simulation files naming convention, between root group name and specific name
	 */
	val CONFIG_SIMULATION_FILE_NAME_SEPARATOR = "@"

	def validateRootFileName(rootFileName: String) = {

		val indexOfAt = rootFileName.indexOf(CONFIG_SIMULATION_FILE_NAME_SEPARATOR)
		val valid = indexOfAt != -1 && (rootFileName.endsWith(SCALA_EXTENSION) || rootFileName.endsWith(TXT_EXTENSION))

		if (!valid)
			logger.warn(" '{}' name doesn't follow the naming convention \"<root name>@<specific name>.(txt|scala). File was skipped.", rootFileName)

		(valid, indexOfAt)
	}

	def getSourceDirectoryNameFromRootFileName(rootFileName: String): Option[String] = {

		val (valid, indexOfAt) = validateRootFileName(rootFileName)

		if (valid)
			Some(rootFileName.substring(0, indexOfAt))
		else
			None
	}

	def getSimulationSpecificName(rootFileName: String): Option[String] = {

		val (valid, indexOfAt) = validateRootFileName(rootFileName)

		if (valid)
			rootFileName match {
				case name if (name.endsWith(SCALA_EXTENSION)) => Some(name.substring(indexOfAt + 1, name.length).dropRight(SCALA_EXTENSION.length))
				case name if (name.endsWith(TXT_EXTENSION)) => Some(name.substring(indexOfAt + 1, name.length).dropRight(TXT_EXTENSION.length))
				case _ => throw new IllegalArgumentException("Unknown file format")
			}
		else
			None
	}
}