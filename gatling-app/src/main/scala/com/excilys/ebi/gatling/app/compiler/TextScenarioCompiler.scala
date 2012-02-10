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
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.io.Source
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{ VirtualFile, Path, AbstractFile }

import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_IMPORTS_FILE
import com.excilys.ebi.gatling.core.util.FileHelper.TXT_EXTENSION
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.core.util.PathHelper.path2jfile
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.core.Conventions

object TextScriptInterpreter {
	val DOLLAR_TEMP_REPLACEMENT = 178.toChar
}
/**
 * Text interpreter used to interpret .txt simulation files
 */
class TextScenarioCompiler extends ScalaScenarioCompiler {

	override def collectSourceFiles(sourceDirectory: Path): List[AbstractFile] = {

		val imports = getClass.getClassLoader.getResources(GATLING_IMPORTS_FILE).map(Source.fromURL(_).mkString)

		val scenario = {
			// Contains the contents of the simulation file
			val initialFileBodyContent = Source.fromFile(sourceDirectory.jfile, CONFIG_ENCODING).mkString.replace('$', TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT)

			// Includes contents of included files into the simulation file 
			"""include\("(.*)"\)""".r.replaceAllIn(initialFileBodyContent,
				result => {
					val sourceDirectoryPath = sourceDirectory.getAbsolutePath
					Conventions.getSourceDirectoryNameFromRootFileName(sourceDirectoryPath).map { sourceDirectoryName =>
						val includePath = sourceDirectoryName / result.group(1) + TXT_EXTENSION
						Source.fromFile(includePath, CONFIG_ENCODING).mkString.replace('$', TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT).replace("\\", "\\\\") + END_OF_LINE + END_OF_LINE
					}.getOrElse(throw new IllegalArgumentException("Couldn't find include replacement"))

				}).replace(TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT, '$')
		}

		val resolvedScenario = {
			val builder = new StringBuilder
			imports.foreach(builder.append(_).append("\n"))
			builder.append("class Simulation extends GatlingSimulation {\n")
			builder.append(scenario).append("\n}")
			builder.toString
		}

		logger.debug(resolvedScenario)

		val file = new VirtualFile("Simulation.scala", "gatling")
		use(file.bufferedOutput)(_.write(resolvedScenario.getBytes))

		List(file)
	}
}