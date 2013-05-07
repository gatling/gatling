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
package io.gatling.app

import java.lang.reflect.Modifier

import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{ Directory, File, Path, PlainFile }
import scala.tools.nsc.io.Path.string2path

import io.gatling.core.scenario.Simulation

object SimulationClassLoader {

	def fromSourcesDirectory(sourceDirectory: Directory): SimulationClassLoader = {

		// Compile the classes
		val classesDir = ZincCompiler(sourceDirectory)

		// Load the compiled classes
		val byteCodeDir = PlainFile.fromPath(classesDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		new FileSystemBackedSimulationClassLoader(classLoader, classesDir)
	}

	def fromClasspathBinariesDirectory(binariesDirectory: Directory) = new FileSystemBackedSimulationClassLoader(getClass.getClassLoader, binariesDirectory)
}

abstract class SimulationClassLoader {

	def simulationClasses(requestedClassName: Option[String]): List[Class[Simulation]]

	protected def isSimulationClass(clazz: Class[_]): Boolean = classOf[Simulation].isAssignableFrom(clazz) && !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)
}

class FileSystemBackedSimulationClassLoader(classLoader: ClassLoader, binaryDir: Directory) extends SimulationClassLoader {

	def simulationClasses(requestedClassName: Option[String]): List[Class[Simulation]] = {

		def pathToClassName(path: Path, root: Path): String = (path.parent / path.stripExtension)
			.toString
			.stripPrefix(root + File.separator)
			.replace(File.separator, ".")

		val classNames = requestedClassName
			.map(List(_))
			.getOrElse {
				binaryDir
					.deepFiles
					.collect { case file if (file.hasExtension("class")) => pathToClassName(file, binaryDir) }
					.toList
			}

		val classes = classNames
			.map(classLoader.loadClass)
			.collect { case clazz if (isSimulationClass(clazz)) => clazz.asInstanceOf[Class[Simulation]] }

		requestedClassName.map { requestedClassName =>
			if (!classes.map(_.getName).contains(requestedClassName)) println(s"Simulation class '$requestedClassName' could not be found.")
		}

		classes
	}
}
