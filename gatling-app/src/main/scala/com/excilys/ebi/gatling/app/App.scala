package com.excilys.ebi.gatling.app

import io.Source

import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.io.Directory
import tools.nsc._
import tools.nsc.util.BatchSourceFile

import java.io.File

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.GraphicsGenerator

class ResultHolder(var value: String)

object App extends Logging {

  def main(args: Array[String]) {

    println("-----------\nGatling cli\n-----------\n")

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

    logger.info("Executing scenario of file '{}'", filename)

    val settings = new Settings
    settings.usejavacp.value = true

    val n = new IMain(settings)

    val fileContent = Source.fromFile("user-files/scenarios/" + filename).mkString + "\n$result__.value = execution"

    val runOn = new ResultHolder("")
    n.bind("$result__", runOn)
    n.interpret(fileContent)
    n.close()

    (new GraphicsGenerator).generateFor(runOn.value)
  }

}
