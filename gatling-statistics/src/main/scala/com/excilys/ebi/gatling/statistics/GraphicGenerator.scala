package com.excilys.ebi.gatling.statistics
import com.excilys.ebi.gatling.statistics.presenter.DataPresenter
import com.excilys.ebi.gatling.statistics.extractor.DataExtractor

class GraphicGenerator[R](dataExtractor: DataExtractor[R], dataPresenter: DataPresenter[R]) {

  def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String) {
    dataExtractor.onRow(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage)
  }

  def generateGraphFor(runOn: String, menuItems: Map[String, String]) {
    val results = dataExtractor.getResults()
    dataPresenter.generateGraphFor(runOn, results, menuItems)
  }
}