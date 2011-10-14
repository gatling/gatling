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

import io.Source
import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.io.Directory
import tools.nsc._
import tools.nsc.util.BatchSourceFile
import scala.util.matching.Regex
import java.io.File
import java.util.Date
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.config.GatlingConfig
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.PropertiesHelper._
import com.excilys.ebi.gatling.core.util.FileHelper._
import com.excilys.ebi.gatling.statistics.generator.GraphicsGenerator
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * Simple Class used to get a value from the interpreter
 */
class DateHolder(var value: DateTime)

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

    // Getting 
    val files = for (
      file <- new Directory(new File(GATLING_SCENARIOS_FOLDER)).files if (!file.name.startsWith(".") && !file.name.startsWith("_"))
    ) yield file.name

    val filesList = files.toList

    // We get the folder name of the run simulation
    val folderName =
      if (!ONLY_STATS_PROPERTY) {
        filesList.size match {
          case 0 =>
            // If there is no simulation file
            logger.warn("There are no scenario scripts. Please verify that your scripts are in user-files/scenarios and that they do not start with a _ or a .")
            sys.exit
          case 1 =>
            // If there is only one simulation file
            logger.info("There is only one scenario, executing it.")
            run(filesList(0))
          case _ =>
            // If there are several simulation files
            println("Which scenario do you want to execute ?")
            var i = 0
            // Prints list of simulation files
            for (filename <- filesList) {
              println("  [" + i + "] " + filename)
              i += 1
            }
            val fileChosen = Console.readInt
            run(filesList(fileChosen))
        }
      } else {
        // If the user wants to execute only statistics generation
        if (args.length > 0) {
          // If there is one argument, take it as folder name
          args(0)
        } else {
          // Else throw an error, as the folder name is required
          logger.error("You specified the property OnlyStats but ommitted the folderName argument.")
          sys.exit
        }
      }

    // Generation of statistics
    if (!NO_STATS_PROPERTY)
      generateStats(folderName)

  }

  /**
   * This method call the statistics module to generate the graphics and statistics
   *
   * @param folderName The folder from which the simulation.log will be parsed
   * @return Nothing
   */
  private def generateStats(folderName: String) = {
    logger.debug("\nGenerating Graphics and Statistics from Folder Name: {}", folderName)

    new GraphicsGenerator().generateFor(folderName)
  }

  /**
   * This method actually runs the simulation by interpreting the scripts.
   *
   * @param fileName The name of the simulation file that will be executed
   * @return The name of the folder of this simulation (ie: its date)
   */
  private def run(fileName: String) = {

    logger.info("Executing simulation of file '{}'", fileName)

    // Sets the interpreter to use the classpath of the java command
    val settings = new Settings
    settings.usejavacp.value = true

    val n = new IMain(settings)

    // This is the file header, with all needed imports and declarations
    val fileHeader = """
    import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
    import com.excilys.ebi.gatling.core.feeder._
    import com.excilys.ebi.gatling.core.context._
    import com.excilys.ebi.gatling.core.util.StringHelper._
    import com.excilys.ebi.gatling.core.runner.Runner._
    import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder._
    import com.excilys.ebi.gatling.core.scenario.builder.ChainBuilder._
    import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.capture.builder.HttpHeaderCaptureBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpXPathCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpRegExpCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpStatusCheckBuilder._
    import com.excilys.ebi.gatling.http.processor.check.builder.HttpHeaderCheckBuilder._
    import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
    import java.util.concurrent.TimeUnit
    import org.joda.time.DateTime
    
    startDate.value = DateTime.now
    
    def runSimulations = runSim(startDate.value)_
    """

    // Contains the contents of the simulation file
    val initialFileBodyContent = Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + fileName).mkString

    // Includes contents of included files into the simulation file 
    val toBeFound = new Regex("""include\("(.*)"\)""")
    val newFileBodyContent = toBeFound replaceAllIn (initialFileBodyContent, result => {
      val partialName = result.group(1)
      var path =
        if (partialName.startsWith("_")) {
          partialName
        } else {
          fileName.substring(0, fileName.length() - 6) + "/" + partialName
        }
      Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + path + SCALA_EXTENSION).mkString + "\n\n"
    })

    // Complete script
    val fileContent = fileHeader + newFileBodyContent
    logger.debug(fileContent)

    val runOn = new DateHolder(DateTime.now)
    n.bind("startDate", runOn)
    n.interpret(fileContent) // This is where the simulation starts
    n.close()

    // Returns the folderName in which the simulation is stored
    DateTimeFormat.forPattern("yyyyMMddHHmmss").print(new DateTime(runOn.value))
  }
}
