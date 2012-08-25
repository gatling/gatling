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

import java.io.{ PrintWriter, StringWriter }
import java.lang.reflect.Modifier

import scala.tools.nsc.{ Global, Settings }
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{ Directory, File, Path }
import scala.tools.nsc.io.PlainFile
import scala.tools.nsc.reporters.ConsoleReporter

import com.excilys.ebi.gatling.core.config.GatlingConfiguration
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import com.excilys.ebi.gatling.core.util.FileHelper
import com.excilys.ebi.gatling.core.util.IOHelper.use

object SimulationClassLoader {

	def fromSourcesDirectory(sourceDirectory: Directory): SimulationClassLoader = {

		val scalaSourceFiles = sourceDirectory.deepFiles.filter(_.hasExtension("scala")).toList

		val binaryDir = FileHelper.createTempDirectory()

		val byteCodeDir = PlainFile.fromPath(binaryDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		val settings = new Settings
		settings.usejavacp.value = true
		settings.outputDirs.setSingleOutput(byteCodeDir)
		settings.deprecation.value = true
		settings.unchecked.value = true
		settings.encoding.value = GatlingConfiguration.configuration.encoding

		// Prepare an object for collecting error messages from the compiler
		val messageCollector = new StringWriter

		use(new PrintWriter(messageCollector)) { pw =>
			// Initialize the compiler
			val reporter = new ConsoleReporter(settings, Console.in, pw)
			val compiler = new Global(settings, reporter)

			(new compiler.Run).compileFiles(scalaSourceFiles.map(PlainFile.fromPath(_)))

			// Bail out if compilation failed
			if (reporter.hasErrors) {
				reporter.printSummary
				throw new RuntimeException("Compilation failed:\n" + messageCollector.toString)
			}
		}

		new FileSystemBackedSimulationClassLoader(classLoader, binaryDir)
	}

	def fromClasspathBinariesDirectory(binariesDirectory: Directory) = new FileSystemBackedSimulationClassLoader(getClass.getClassLoader, binariesDirectory)
}

abstract class SimulationClassLoader {

	def simulationClasses(explicitClassNames: Option[List[String]] = None): List[Class[Simulation]]

	protected val isSimulationClass = (clazz: Class[_]) => classOf[Simulation].isAssignableFrom(clazz) && !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)
}

class FileSystemBackedSimulationClassLoader(classLoader: ClassLoader, binaryDir: Directory) extends SimulationClassLoader {

	private def pathToClassName(path: Path, root: Path): String = (path.parent / path.stripExtension)
		.toString
		.stripPrefix(root + File.separator)
		.replace(File.separator, ".")

	def simulationClasses(requestedClassNames: Option[List[String]] = None): List[Class[Simulation]] = {

		val classNames = requestedClassNames.getOrElse {
			binaryDir
				.deepFiles
				.filter(_.hasExtension("class"))
				.map(pathToClassName(_, binaryDir))
		}

		val classes = classNames.map(classLoader.loadClass(_))
			.filter(isSimulationClass)
			.map(_.asInstanceOf[Class[Simulation]])
			.toList

		requestedClassNames.map { requestedClassNames =>
			val loadedClassNames = classes.map(_.getName)
			val notFounds = requestedClassNames.filterNot(classes.contains(_))
			if (!notFounds.isEmpty)
				println("The following simulation names didn't match any Simulation class name and were filtered out: " + notFounds)
		}

		classes
	}
}
