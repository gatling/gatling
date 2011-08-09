package com.excilys.ebi.gatling.app

import io.Source

import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.io.Directory
import tools.nsc._
import tools.nsc.util.BatchSourceFile
import scala.util.matching.Regex

import java.io.File

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.config.GatlingConfig

import com.excilys.ebi.gatling.statistics.GraphicsGenerator

class ResultHolder(var value: String)

object App extends Logging {

  def main(args: Array[String]) {

    println("-----------\nGatling cli\n-----------\n")

    GatlingConfig // Initializes configuration

    val files = for (
      file <- new Directory(new File("user-files/scenarios")).files if (!file.name.startsWith(".") && !file.name.startsWith("_"))
    ) yield file.name

    val filesList = files.toList

    filesList.size match {
      case 0 => logger.info("There are no scenario scripts. Please verify that your scripts are in user-files/scenarios and that they do not start with a _ or a .")
      case 1 =>
        logger.info("There is only one scenario, executing it.")
        runAndGenerateStats(filesList(0))
      case _ =>
        println("Which scenario do you want to execute ?")
        var i = 0
        for (filename <- filesList) {
          println("  [" + i + "] " + filename)
          i += 1
        }
        val fileChosen = Console.readInt
        runAndGenerateStats(filesList(fileChosen))
    }

  }

  private def runAndGenerateStats(filename: String) = {

    logger.info("Executing simulation of file '{}'", filename)

    val settings = new Settings
    settings.usejavacp.value = true

    val n = new IMain(settings)

    val initialFileBodyContent = Source.fromFile("user-files/scenarios/" + filename).mkString

    val toBeFound = new Regex("""scenarioFromFile\("(.*)"\)""")
    val newFileBodyContent = toBeFound replaceAllIn (initialFileBodyContent, result =>
      Source.fromFile("user-files/scenarios/_" + result.group(1) + ".scala").mkString + "\n\n")

    val imports = """
import com.excilys.ebi.gatling.core.feeder._
import com.excilys.ebi.gatling.core.context._
import com.excilys.ebi.gatling.core.scenario.configuration.builder.ScenarioConfigurationBuilder._
import com.excilys.ebi.gatling.http.scenario.builder.HttpScenarioBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpRegExpCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpXPathCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpHeaderCaptureBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpXPathAssertionBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpRegExpAssertionBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpStatusAssertionBuilder._
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpHeaderAssertionBuilder._
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder._
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder._
import com.excilys.ebi.gatling.http.header.HeaderKey._
import com.excilys.ebi.gatling.http.runner.HttpRunner._
import java.util.concurrent.TimeUnit
"""

    val fileContent = imports + newFileBodyContent + "\n\n$result__.value = execution"

    logger.debug(fileContent)

    val runOn = new ResultHolder("")
    n.bind("$result__", runOn)
    n.interpret(fileContent)
    n.close()

    logger.debug("RunOn Value: {}", runOn.value)

    (new GraphicsGenerator).generateFor(runOn.value)
  }

}
