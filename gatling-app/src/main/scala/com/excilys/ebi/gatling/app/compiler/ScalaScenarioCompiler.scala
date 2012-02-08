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
package com.excilys.ebi.gatling.app.compiler
import java.io.{ StringWriter, PrintWriter }

import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{ VirtualDirectory, PlainFile, Path, Directory, AbstractFile }
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.{ Settings, Global }

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_SIMULATION_SCALA_PACKAGE
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_SIMULATIONS_FOLDER
import com.excilys.ebi.gatling.core.util.PathHelper.path2jfile
import com.excilys.ebi.gatling.core.util.ReflectionHelper.getNewInstanceByClassName
import com.excilys.ebi.gatling.core.util.Resource.use
import com.excilys.ebi.gatling.core.Conventions

/**
 * This class is used to interpret scala simulations
 */
class ScalaScenarioCompiler extends ScenarioCompiler {

	val byteCodeDir = new VirtualDirectory("memory", None)
	val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

	/**
	 * This method launches the interpretation of the simulation and runs it
	 *
	 * @param fileName the name of the file containing the simulation description
	 * @param startDate the date at which the launch was asked
	 */
	def run(fileName: String, startDate: DateTime) {
		compile(GATLING_SIMULATIONS_FOLDER / fileName)
		val runner = getNewInstanceByClassName[App](CONFIG_SIMULATION_SCALA_PACKAGE + "Simulation", classLoader)
		runner.main(Array(startDate.toString));
	}

	/**
	 * Compiles all the files needed for the simulation
	 *
	 * @param sourceDirectory the file containing the simulation description
	 */
	def compile(sourceDirectory: Path): Unit = {

		// Attempt compilation
		val files = collectSourceFiles(sourceDirectory)

		// Prepare an object for collecting error messages from the compiler
		val messageCollector = new StringWriter

		use(new PrintWriter(messageCollector)) { pw =>
			// Initialize the compiler
			val settings = generateSettings
			val reporter = new ConsoleReporter(settings, Console.in, pw)
			val compiler = new Global(settings, reporter)

			(new compiler.Run).compileFiles(files)

			// Bail out if compilation failed
			if (reporter.hasErrors) {
				reporter.printSummary
				throw new RuntimeException("Compilation failed:\n" + messageCollector.toString)
			}
		}
	}

	def collectSourceFiles(sourceDirectory: Path): List[AbstractFile] = {
		if (sourceDirectory.isFile) {
			val rootFile = PlainFile.fromPath(sourceDirectory)
			Conventions.getSourceDirectoryNameFromRootFileName(sourceDirectory.getAbsolutePath).map { sourceDirectoryName =>
				val dir = Directory(sourceDirectoryName)
				if (dir.exists)
					rootFile :: dir.walk.map(PlainFile.fromPath(_)).toList
				else
					List(rootFile)
			}.getOrElse(Nil)
		} else
			sourceDirectory.walk.map(PlainFile.fromPath(_)).toList
	}

	/**
	 * Generates the settings of the scala compiler
	 */
	private def generateSettings: Settings = {
		val settings = new Settings
		settings.usejavacp.value = true
		settings.outputDirs.setSingleOutput(byteCodeDir)
		settings.deprecation.value = true
		settings.unchecked.value = true
		settings
	}
}