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

class DateHolder(var value: DateTime)

object App extends Logging {

  def main(args: Array[String]) {

    println("-----------\nGatling cli\n-----------\n")

    GatlingConfig // Initializes configuration

    val files = for (
      file <- new Directory(new File(GATLING_SCENARIOS_FOLDER)).files if (!file.name.startsWith(".") && !file.name.startsWith("_"))
    ) yield file.name

    val filesList = files.toList

    var folderName = StringUtils.EMPTY

    if (!ONLY_STATS_PROPERTY) {
      folderName = filesList.size match {
        case 0 =>
          logger.warn("There are no scenario scripts. Please verify that your scripts are in user-files/scenarios and that they do not start with a _ or a .")
          sys.exit
        case 1 =>
          logger.info("There is only one scenario, executing it.")
          run(filesList(0))
        case _ =>
          println("Which scenario do you want to execute ?")
          var i = 0
          for (filename <- filesList) {
            println("  [" + i + "] " + filename)
            i += 1
          }
          val fileChosen = Console.readInt
          run(filesList(fileChosen))
      }
    } else {
      if (args.length > 0) {
        folderName = args(0)
      } else {
        logger.error("You specified the property OnlyStats but ommitted the folderName argument.")
        sys.exit
      }
    }

    if (!NO_STATS_PROPERTY)
      generateStats(folderName)

  }

  private def generateStats(folderName: String) = {
    logger.debug("\nGenerating Graphics and Statistics from Folder Name: {}", folderName)

    new GraphicsGenerator().generateFor(folderName)
  }

  private def run(filename: String) = {

    logger.info("Executing simulation of file '{}'", filename)

    val settings = new Settings
    settings.usejavacp.value = true

    val n = new IMain(settings)

    val initialFileBodyContent = Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + filename).mkString

    val toBeFound = new Regex("""include\("(.*)"\)""")
    val newFileBodyContent = toBeFound replaceAllIn (initialFileBodyContent, result => {
      val partialName = result.group(1)
      var path =
        if (partialName.startsWith("_")) {
          partialName
        } else {
          filename.substring(0, filename.length() - 6) + "/" + partialName
        }
      Source.fromFile(GATLING_SCENARIOS_FOLDER + "/" + path + SCALA_EXTENSION).mkString + "\n\n"
    })

    val fileHeader = """
import com.excilys.ebi.gatling.core.feeder._
import com.excilys.ebi.gatling.core.context._
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._
import com.excilys.ebi.gatling.http.scenario.builder.HttpScenarioBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpHeaderCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpXPathCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpRegExpCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpStatusCheckBuilder._
import com.excilys.ebi.gatling.http.processor.check.builder.HttpHeaderCheckBuilder._
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder._
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder._
import com.excilys.ebi.gatling.http.runner.HttpRunner._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

startDate.value = DateTime.now

def runSimulations = runSim(startDate.value)_
"""

    val fileContent = fileHeader + newFileBodyContent
    logger.debug(fileContent)

    val runOn = new DateHolder(DateTime.now)
    n.bind("startDate", runOn)
    n.interpret(fileContent)
    n.close()

    DateTimeFormat.forPattern("yyyyMMddHHmmss").print(new DateTime(runOn.value))
  }
}
