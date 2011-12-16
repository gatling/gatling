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
package com.excilys.ebi.gatling.app.interpreter

import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.Settings

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.config.GatlingConfig.CONFIG_ENCODING
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ GATLING_SCENARIOS_FOLDER, GATLING_IMPORTS_FILE }
import com.excilys.ebi.gatling.core.util.FileHelper.TXT_EXTENSION
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

/**
 * Simple Class used to get a value from the interpreter
 */
class DateHolder(var value: DateTime)

object TextScriptInterpreter {
	val DOLLAR_TEMP_REPLACEMENT = 178.toChar
}
/**
 * Text interpreter used to interpret .txt simulation files
 */
class TextScriptInterpreter extends Interpreter {
	/**
	 * This method launches the interpretation of the simulation and runs it
	 *
	 * @param fileName the name of the file containing the simulation description
	 * @param startDate the date at which the launch was asked
	 */
	def run(fileName: String, startDate: DateTime) = {

		val interpreter = {
			// Sets the interpreter to use the classpath of the java command
			val settings = new Settings
			settings.usejavacp.value = true

			new IMain(settings)
		}

		val imports =
			getClass.getClassLoader.getResources(GATLING_IMPORTS_FILE).map { resource =>
				Source.fromURL(resource).mkString
			}

		val scenario = {
			// Contains the contents of the simulation file
			val initialFileBodyContent = Source.fromFile((GATLING_SCENARIOS_FOLDER / fileName).jfile, CONFIG_ENCODING).mkString.replace('$', TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT)

			// Includes contents of included files into the simulation file 
			"""include\("(.*)"\)""".r.replaceAllIn(initialFileBodyContent,
				result => {
					val path = fileName.substring(0, fileName.lastIndexOf("@")) / result.group(1)
					Source.fromFile(GATLING_SCENARIOS_FOLDER / path + TXT_EXTENSION, CONFIG_ENCODING).mkString.replace('$', TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT) + END_OF_LINE + END_OF_LINE
				}).replace(TextScriptInterpreter.DOLLAR_TEMP_REPLACEMENT, '$')
		}

		logger.debug(scenario)

		interpreter.bind("startDate", new DateHolder(startDate))

		imports.foreach(interpreter.interpret(_))
		interpreter.interpret(scenario)
		interpreter.close
	}
}