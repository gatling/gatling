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
package com.excilys.ebi.gatling.app

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY

object CommandLineOptions extends Logging {
	private var clo: CommandLineOptions = null
	private var reportsOnly = false
	private var reportsOnlyFolder = EMPTY
	private var noReports = false

	private var configFileName: Option[String] = None
	private var resultsFolder: Option[String] = None
	private var dataFolder: Option[String] = None
	private var requestBodiesFolder: Option[String] = None
	private var simulationFolder: Option[String] = None
	private var simulationPackage: Option[String] = None
	private var assetsFolder: Option[String] = None
	private var simulations: Option[List[String]] = None

	def setReportsOnly(reportsOnlyFolder: String) = {
		this.reportsOnly = true
		this.reportsOnlyFolder = reportsOnlyFolder
	}
	def setNoReports = { this.noReports = true }
	def setConfigFileName(configFileName: String) = { this.configFileName = Some(configFileName) }
	def setDataFolder(dataFolder: String) = { this.dataFolder = Some(dataFolder) }
	def setRequestBodiesFolder(requestBodiesFolder: String) = { this.requestBodiesFolder = Some(requestBodiesFolder) }
	def setResultsFolder(resultsFolder: String) = { this.resultsFolder = Some(resultsFolder) }
	def setAssetsFolder(assetsFolder: String) = { this.assetsFolder = Some(assetsFolder) }
	def setSimulationFolder(simulationFolder: String) = { this.simulationFolder = Some(simulationFolder) }
	def setSimulationPackage(simulationPackage: String) = { this.simulationPackage = Some(simulationPackage) }
	def setSimulations(simulations: String) = { this.simulations = Some(simulations.split(",").toList) }
	
	lazy val options =
		new CommandLineOptions(reportsOnly, reportsOnlyFolder, noReports, configFileName, dataFolder, requestBodiesFolder, resultsFolder, simulationFolder,
			simulationPackage, assetsFolder, simulations)
}
class CommandLineOptions(val reportsOnly: Boolean, val reportsOnlyFolder: String, val noReports: Boolean, val configFileName: Option[String], val dataFolder: Option[String],
		val requestBodiesFolder: Option[String], val resultsFolder: Option[String], val simulationFolder: Option[String], val simulationPackage: Option[String],
		val assetsFolder: Option[String], val simulations: Option[List[String]]) {
	override def toString =
		new StringBuilder()
			.append("reportsOnly: ").append(reportsOnly)
			.append(", reportsOnlyFolder: ").append(reportsOnlyFolder)
			.append(", noReports: ").append(noReports)
			.append(", configFileName: ").append(configFileName)
			.append(", resultsFolder: ").append(resultsFolder)
			.append(", dataFolder: ").append(dataFolder)
			.append(", requestBodiesFolder: ").append(requestBodiesFolder)
			.append(", simulationFolder: ").append(simulationFolder)
			.append(", simulationPackage: ").append(simulationPackage)
			.append(", assetsFolder: ").append(assetsFolder)
			.append(", simulations: ").append(simulations)
			.toString
}