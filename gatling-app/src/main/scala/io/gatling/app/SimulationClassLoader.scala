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
import scala.tools.nsc.io.{ Directory, File, Path }
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.PlainFile

import io.gatling.core.scenario.Simulation

object SimulationClassLoader {

	def fromSourcesDirectory(sourceDirectory: Directory): SimulationClassLoader = {

		// Compile the classes
		val classesDir = ZincCompilerLauncher(sourceDirectory)

		// Pass the compiled classes to a ClassLoader
		val byteCodeDir = PlainFile.fromPath(classesDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		new SimulationClassLoader(classLoader, classesDir)
	}

	def fromClasspathBinariesDirectory(binariesDirectory: Directory) = new SimulationClassLoader(getClass.getClassLoader, binariesDirectory)
}

class SimulationClassLoader(classLoader: ClassLoader, binaryDir: Directory) {

	def simulationClasses(requestedClassName: Option[String]): List[Class[Simulation]] = {

		def isSimulationClass(clazz: Class[_]): Boolean = classOf[Simulation].isAssignableFrom(clazz) && !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)

		def pathToClassName(path: Path, root: Path): String = (path.parent / path.stripExtension)
			.toString
			.stripPrefix(root + File.separator)
			.replace(File.separator, ".")

		requestedClassName.map { requestedClassName =>
			val clazz = classLoader.loadClass(requestedClassName)
			assert(isSimulationClass(clazz), s"Requested class name $requestedClassName does not extend Simulation")
			List(clazz.asInstanceOf[Class[Simulation]])

		}.getOrElse {
			binaryDir
				.deepFiles
				.collect { case file if file.hasExtension("class") => classLoader.loadClass(pathToClassName(file, binaryDir)) }
				.collect { case clazz if isSimulationClass(clazz) => clazz.asInstanceOf[Class[Simulation]] }
				.toList
		}
	}
}
