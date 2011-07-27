package com.excilys.ebi.gatling.app

import io.Source

import tools.nsc.interpreter.IMain
import tools.nsc.Settings
import tools.nsc.io.Directory
import tools.nsc._
import tools.nsc.util.BatchSourceFile

import java.io.File

import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.statistics.presenter.ActiveSessionsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.GlobalRequestsDataPresenter
import com.excilys.ebi.gatling.statistics.presenter.DetailsRequestsDataPresenter

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

    compileAndRun(filesList(fileChosen))
  }

  private def compileAndRun(filename: String) = {

    val settings = new Settings
    settings.usejavacp.value = true
    val n = new IMain(settings)

    val fileContent = Source.fromFile("user-files/scenarios/" + filename).mkString + "\n$result__.value = execution"

    logger.debug(fileContent)

    val runOn = new ResultHolder("")
    n.bind("$result__", runOn)

    n.interpret(fileContent)

    logger.debug("result: {}", runOn.value)

    n.close()

    generateStatistics(runOn.value)
  }

  private def generateStatistics(runOn: String) {
    val detailsRequestsPresenter = new DetailsRequestsDataPresenter
    val menuItems = detailsRequestsPresenter.generateGraphFor(runOn)

    val activeSessionsPresenter = new ActiveSessionsDataPresenter
    activeSessionsPresenter.generateGraphFor(runOn, menuItems)

    val requestsPresenter = new GlobalRequestsDataPresenter
    requestsPresenter.generateGraphFor(runOn, menuItems)
  }

}
