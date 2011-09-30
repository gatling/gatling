package com.excilys.ebi.gatling.statistics.generator

class CompositeGraphicGenerator(generators: GraphicGenerator*) extends GraphicGenerator {

  def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String) {
    generators.foreach { generator =>
      generator.onRow(runOn, scenarioName, userId, actionName, executionStartDate, executionDuration, resultStatus, resultMessage)
    }
  }

  def generateGraphFor(runOn: String) {
    generators.foreach { generator =>
      generator.generateGraphFor(runOn)
    }
  }
}