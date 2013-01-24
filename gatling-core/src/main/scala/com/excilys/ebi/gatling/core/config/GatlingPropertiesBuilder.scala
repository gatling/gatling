/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.util.{ HashMap => JHashMap, Map => JMap }

import scala.collection.JavaConversions.seqAsJavaList

import com.excilys.ebi.gatling.core.ConfigurationConstants._

class GatlingPropertiesBuilder {

	private val props: JMap[String, Any] = new JHashMap[String, Any]

	def noReports {
		props.put(CONF_CHARTING_NO_REPORTS, true)
	}

	def reportsOnly(v: String) {
		props.put(CONF_DIRECTORY_REPORTS_ONLY, v)
	}

	def dataDirectory(v: String) {
		props.put(CONF_DIRECTORY_DATA, v)
	}

	def resultsDirectory(v: String) {
		props.put(CONF_DIRECTORY_RESULTS, v)
	}

	def requestBodiesDirectory(v: String) {
		props.put(CONF_DIRECTORY_REQUEST_BODIES, v)
	}

	def sourcesDirectory(v: String) {
		props.put(CONF_DIRECTORY_SIMULATIONS, v)
	}

	def binariesDirectory(v: String) {
		props.put(CONF_DIRECTORY_BINARIES, v)
	}

	def clazz(v: String) {
		props.put(CONF_SIMULATION_CLASS, v)
	}

	def outputDirectoryBaseName(v: String) {
		props.put(CONF_SIMULATION_OUTPUT_DIRECTORY_BASE_NAME, v)
	}

	def build = props
}