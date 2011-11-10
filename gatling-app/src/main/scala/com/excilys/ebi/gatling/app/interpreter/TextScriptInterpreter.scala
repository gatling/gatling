/**
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
package com.excilys.ebi.gatling.app.interpreter

import scala.io.Source
import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.Settings
import scala.util.matching.Regex

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.util.FileHelper.TXT_EXTENSION
import com.excilys.ebi.gatling.core.util.PathHelper.GATLING_SCENARIOS_FOLDER

/**
 * Simple Class used to get a value from the interpreter
 */
class DateHolder(var value: DateTime)

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
		// Sets the interpreter to use the classpath of the java command
		val settings = new Settings
		settings.usejavacp.value = true

		val n = new IMain(settings)

		// This is the file header, with all needed imports and declarations
		val fileHeader = """
    import com.excilys.ebi.gatling.core.Predef._
    import com.excilys.ebi.gatling.http.Predef._
    
    def runSimulations = runSimFunction(startDate.value)
    """

		// Contains the contents of the simulation file
		val initialFileBodyContent = Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + fileName).mkString

		// Includes contents of included files into the simulation file 
		val toBeFound = new Regex("""include\("(.*)"\)""")
		val newFileBodyContent = toBeFound.replaceAllIn(initialFileBodyContent, result => {
			val path = fileName.substring(0, fileName.lastIndexOf("@")) + "/" + result.group(1)
			Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + path + TXT_EXTENSION).mkString + "\n\n"
		})

		// Complete script
		val fileContent = fileHeader + newFileBodyContent
		logger.debug(fileContent)

		n.bind("startDate", new DateHolder(startDate))
		n.interpret(fileContent) // This is where the simulation starts
		n.close()
	}
}