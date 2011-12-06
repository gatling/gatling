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

import scala.io.Codec
import scala.tools.nsc.io.Path.string2path
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import GatlingFiles.GATLING_DEFAULT_CONFIG_FILE
import scala.tools.nsc.io.Path

/**
 * Configuration loader of Gatling
 */
object GatlingConfig extends Logging {

	private var configFileName: Option[String] = None
	private var dataFolder: Option[String] = None
	private var requestBodiesFolder: Option[String] = None
	private var resultsFolder: Option[String] = None

	def apply(configFileName: Option[String], dataFolder: Option[String], requestBodiesFolder: Option[String], resultsFolder: Option[String]) = {
		this.configFileName = configFileName
		this.dataFolder = dataFolder
		this.requestBodiesFolder = requestBodiesFolder
		this.resultsFolder = resultsFolder
	}

	/**
	 * Contains the configuration of Gatling
	 */
	val config: GatlingConfiguration =
		try {
			// Locate configuration file, depending on users options
			val configFile =
				configFileName map { fileName =>
					logger.info("Loading custom configuration file: conf/{}", fileName)
					fileName
				} getOrElse {
					logger.info("Loading default configuration file")
					GATLING_DEFAULT_CONFIG_FILE
				}

			GatlingConfiguration.fromFile(configFile)
		} catch {
			case e =>
				logger.error("{}\n{}", e.getMessage, e.getStackTraceString)
				throw new Exception("Could not parse configuration file!")
		}

	lazy val CONFIG_RESULTS_FOLDER: Option[Path] = resultsFolder.map(s => s)
	lazy val CONFIG_DATA_FOLDER: Option[Path] = dataFolder.map(s => s)
	lazy val CONFIG_REQUEST_BODIES_FOLDER: Option[Path] = requestBodiesFolder.map(s => s)

	/**
	 * Gatling global encoding value
	 */
	val CONFIG_ENCODING = config("gatling.encoding", Codec.UTF8.name)
	/**
	 * Gatling simulation timeout value
	 */
	val CONFIG_SIMULATION_TIMEOUT = config("gatling.simulation.timeout", 86400)

	val CONFIG_SIMULATION_SCALA_PACKAGE = config("gatling.simulation.scalaPackage", EMPTY)

	val CONFIG_CHARTING_INDICATORS_LOWER_BOUND = config("gatling.charting.indicators.lowerBound", 100)

	val CONFIG_CHARTING_INDICATORS_HIGHER_BOUND = config("gatling.charting.indicators.higherBound", 500)
}
