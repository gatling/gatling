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

    println("Which scenario do you want to execute ?")

    var i = 0
    for (filename <- filesList) {
      println("  [" + i + "] " + filename)
      i += 1
    }

    val fileChosen = Console.readInt

    runAndGenerateStats(filesList(fileChosen))
  }

  private def runAndGenerateStats(filename: String) = {

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
