package com.excilys.ebi.gatling.statistics.generator

trait GraphicGenerator {

  def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String, groups: List[String])

  def generateGraphFor(runOn: String)
}