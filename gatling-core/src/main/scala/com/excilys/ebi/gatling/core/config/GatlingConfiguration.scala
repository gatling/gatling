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

import java.util.concurrent.atomic.AtomicBoolean

import scala.io.Codec
import scala.tools.nsc.io.Path

import com.excilys.ebi.gatling.core.result.reader.DataReader
import com.excilys.ebi.gatling.core.result.writer.{ ConsoleDataWriter, DataWriter, FileDataWriter }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

import grizzled.slf4j.Logging

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends Logging {

	private val initialized = new AtomicBoolean(false)

	val GATLING_DEFAULT_CONFIG_FILE = "gatling.conf"

	@volatile private var instance: GatlingConfiguration = _

	def setUp(configFileName: Option[String], dataFolder: Option[String], requestBodiesFolder: Option[String], resultsFolder: Option[String], simulationsFolder: Option[String]) {
		if (initialized.compareAndSet(false, true))
			instance = GatlingConfiguration(configFileName, dataFolder, requestBodiesFolder, resultsFolder, simulationsFolder)
		else
			throw new UnsupportedOperationException("GatlingConfig already set up")
	}

	def configuration = if (initialized.get) instance else throw new UnsupportedOperationException("Can't access configuration instance if it hasn't been set up")

	def apply(configFilePath: Option[String], dataDirectoryPathString: Option[String], requestBodiesDirectoryPathString: Option[String], resultsDirectoryPathString: Option[String], simulationsDirectoryPathString: Option[String]) = {

		val fileConfiguration: GatlingFileConfiguration =
			try {
				// Locate configuration file, depending on users options
				val configFile = configFilePath.map { path =>
					info("Loading custom configuration file: " + path)
					path
				} getOrElse {
					info("Loading default configuration file")
					GATLING_DEFAULT_CONFIG_FILE
				}

				GatlingFileConfiguration.fromFile(configFile)
			} catch {
				case e => throw new RuntimeException("Could not parse configuration file!", e)
			}

		val resultsDirectoryPath: Option[Path] = resultsDirectoryPathString.map(Path(_))
		val dataDirectoryPath: Option[Path] = dataDirectoryPathString.map(Path(_))
		val requestBodiesDirectoryPath: Option[Path] = requestBodiesDirectoryPathString.map(Path(_))
		val simulationsDirectoryPath: Option[Path] = simulationsDirectoryPathString.map(Path(_))

		new GatlingConfiguration(fileConfiguration, dataDirectoryPath, requestBodiesDirectoryPath, resultsDirectoryPath, simulationsDirectoryPath)
	}
}

class GatlingConfiguration(
		val fileConfiguration: GatlingFileConfiguration,
		val dataDirectoryPath: Option[Path],
		val requestBodiesDirectoryPath: Option[Path],
		val resultsDirectoryPath: Option[Path],
		val simulationsDirectoryPath: Option[Path]) extends Logging {

	/**
	 * Contains the configuration of Gatling
	 */

	/**
	 * Gatling global encoding value
	 */
	val encoding = fileConfiguration("gatling.encoding", Codec.UTF8.name)

	/**
	 * Gatling simulation timeout value
	 */
	val simulationTimeOut = fileConfiguration("gatling.simulation.timeout", 86400)

	val simulationScalaPackage = fileConfiguration("gatling.simulation.scalaPackage", EMPTY)

	val chartingIndicatorsLowerBound = fileConfiguration("gatling.charting.indicators.lowerBound", 800)

	val chartingIndicatorsHigherBound = fileConfiguration("gatling.charting.indicators.higherBound", 1200)

	val chartingIndicatorsPercentile1 = fileConfiguration("gatling.charting.indicators.percentile1", 95)

	val chartingIndicatorsPercentile2 = fileConfiguration("gatling.charting.indicators.percentile2", 99)

	val chartingMaxPlotPerSerie = fileConfiguration("gatling.charting.maxPlotPerSerie", 5000)

	lazy val dataWriterClasses = {
		val classes = fileConfiguration.getList("gatling.data.writers").map { className => Class.forName(className).asInstanceOf[Class[DataWriter]] }

		if (classes.isEmpty)
			List(classOf[ConsoleDataWriter], classOf[FileDataWriter])

		else
			classes
	}

	lazy val dataReaderClass = Class.forName(fileConfiguration("gatling.data.reader", "com.excilys.ebi.gatling.charts.result.reader.FileDataReader")).asInstanceOf[Class[DataReader]]
}
