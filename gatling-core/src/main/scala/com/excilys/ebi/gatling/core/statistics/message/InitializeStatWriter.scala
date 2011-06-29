package com.excilys.ebi.gatling.core.statistics.message

import java.util.Date

case class InitializeStatWriter(val runOn: Date, val scenarioName: String, val numberOfRelevantActions: Int)