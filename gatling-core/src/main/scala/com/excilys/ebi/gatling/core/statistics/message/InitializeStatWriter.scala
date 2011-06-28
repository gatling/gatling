package com.excilys.ebi.gatling.core.statistics.message

case class InitializeStatWriter(val runOn: String, val scenarioName: String, val numberOfRelevantActions: Integer)