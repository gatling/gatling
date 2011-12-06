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

import GatlingFiles.{ GATLING_CONFIG_FOLDER, GATLING_DEFAULT_CONFIG_FILE }

/**
 * Configuration loader of Gatling
 */
object GatlingConfig extends Logging {

	private var configFileName = EMPTY

	def apply(configFileName: String) = {
		this.configFileName = configFileName
	}

	/**
	 * Contains the configuration of Gatling
	 */
	val config: GatlingConfiguration =
		try {
			// Locate configuration file, depending on users options
			val configFile =
				if (configFileName != EMPTY) {
					logger.info("Loading custom configuration file: conf/{}", configFileName)
					configFileName
				} else {
					logger.info("Loading default configuration file")
					GATLING_DEFAULT_CONFIG_FILE
				}

			GatlingConfiguration.fromFile(configFile.toString)
		} catch {
			case e =>
				logger.error("{}\n{}", e.getMessage, e.getStackTraceString)
				throw new Exception("Could not parse configuration file.")
		}

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
