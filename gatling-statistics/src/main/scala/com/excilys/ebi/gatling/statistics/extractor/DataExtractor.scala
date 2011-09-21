package com.excilys.ebi.gatling.statistics.extractor

trait DataExtractor[R] {

  def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String)

  def getResults(): R
}