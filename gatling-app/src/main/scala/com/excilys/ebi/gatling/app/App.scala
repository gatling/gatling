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
package com.excilys.ebi.gatling.app

import java.io.File

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Set => MSet }
import scala.collection.mutable.{ MultiMap, HashMap }
import scala.tools.nsc.io.Directory

import org.joda.time.DateTime

import com.excilys.ebi.gatling.app.interpreter.{ TextScriptInterpreter, ScalaScriptInterpreter }
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.DateHelper.printFileNameDate
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_SCENARIOS_FOLDER
import com.excilys.ebi.gatling.core.util.PropertiesHelper.{ ONLY_STATS_PROPERTY, NO_STATS_PROPERTY }
import com.excilys.ebi.gatling.statistics.generator.ChartsGenerator

/**
 * Object containing entry point of application
 */
object App extends Logging {

	/**
	 * Entry point of Application
	 *
	 * @param args Arguments of the main method
	 */
	def main(args: Array[String]) {

		println("-----------\nGatling cli\n-----------\n")

		GatlingConfig // Initializes configuration

		// Getting files in scenarios folder
		val files = for (
			file <- new Directory(new File(GATLING_SCENARIOS_FOLDER)).files if (!file.name.startsWith("."))
		) yield file.name

		val (files1, files2) = files.duplicate

		// Sorting file names by radical and storing groups for display purpose
		val sortedFiles: MultiMap[String, String] = new HashMap[String, MSet[String]] with MultiMap[String, String]
		var sortedGroups: TreeSet[String] = new TreeSet[String]

		for (fileName <- files1) {
			sortedFiles.addBinding(fileName.substring(0, fileName.indexOf("@")), fileName)
			sortedGroups += fileName.substring(0, fileName.indexOf("@"))
		}

		// We get the folder name of the run simulation
		val folderName =
			if (!ONLY_STATS_PROPERTY) {
				files2.size match {
					case 0 =>
						// If there is no simulation file
						logger.warn("There are no scenario scripts. Please verify that your scripts are in user-files/scenarios and that they do not start with a _ or a .")
						sys.exit
					case 1 =>
						// If there is only one simulation file
						logger.info("There is only one scenario, executing it.")
						run(files.next)
					case _ =>
						// If there are several simulation files
						println("Which scenario do you want to execute ?")

						var i = 0
						var filesList: List[String] = Nil

						for (group <- sortedGroups) {
							println("\n - " + group)
							sortedFiles.get(group).map { set =>
								for (fileName <- set) {
									println("     [" + i + "] " + fileName.substring(fileName.indexOf("@") + 1, fileName.indexOf(".")))
									filesList = fileName :: filesList
									i += 1
								}
							}
						}

						val fileChosen = Console.readInt
						run(filesList.reverse(fileChosen))
				}
			} else if (args.length > 0) {
				// If the user wants to execute only statistics generation

				// If there is one argument, take it as folder name
				args(0)
			} else {
				// Else throw an error, as the folder name is required
				logger.error("You specified the property OnlyStats but ommitted the folderName argument.")
				sys.exit
			}

		// Generation of statistics
		if (!NO_STATS_PROPERTY)
			generateStats(folderName)

	}

	/**
	 * This method call the statistics module to generate the charts and statistics
	 *
	 * @param folderName The folder from which the simulation.log will be parsed
	 * @return Nothing
	 */
	private def generateStats(folderName: String) = {
		logger.debug("\nGenerating Charts and Statistics from Folder Name: {}", folderName)

		new ChartsGenerator().generateFor(folderName)
	}

	/**
	 * This method actually runs the simulation by interpreting the scripts.
	 *
	 * @param fileName The name of the simulation file that will be executed
	 * @return The name of the folder of this simulation (ie: its date)
	 */
	private def run(fileName: String) = {

		logger.info("Executing simulation of file '{}'", fileName)

		val startDate = DateTime.now
		val interpreter = fileName match {
			case fn if (fn.endsWith(".scala")) => new ScalaScriptInterpreter()
			case fn if (fn.endsWith(".txt")) => new TextScriptInterpreter()
			case _ => throw new UnsupportedOperationException
		}

		interpreter.run(fileName, startDate)

		// Returns the folderName in which the simulation is stored
		printFileNameDate(startDate)
	}
}
