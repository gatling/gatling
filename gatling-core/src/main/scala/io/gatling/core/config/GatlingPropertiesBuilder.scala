/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.config

import scala.collection.mutable

import io.gatling.core.ConfigurationConstants._

class GatlingPropertiesBuilder {

	private val props = mutable.Map.empty[String, Any]

	def noReports {
		props += CONF_CHARTING_NO_REPORTS -> true
	}

	def reportsOnly(v: String) { props += CONF_CORE_DIRECTORY_REPORTS_ONLY -> v }

	def dataDirectory(v: String) { props += CONF_CORE_DIRECTORY_DATA -> v }

	def resultsDirectory(v: String) { props += CONF_CORE_DIRECTORY_RESULTS -> v }

	def requestBodiesDirectory(v: String) { props += CONF_CORE_DIRECTORY_REQUEST_BODIES -> v }

	def sourcesDirectory(v: String) { props += CONF_CORE_DIRECTORY_SIMULATIONS -> v }

	def binariesDirectory(v: String) { props += CONF_CORE_DIRECTORY_BINARIES -> v }

	def simulationClass(v: String) { props += CONF_CORE_SIMULATION_CLASS -> v }

	def outputDirectoryBaseName(v: String) { props += CONF_CORE_OUTPUT_DIRECTORY_BASE_NAME -> v }

	def runDescription(v: String) { props.put(CONF_CORE_RUN_DESCRIPTION, v) }

	def build = props
}