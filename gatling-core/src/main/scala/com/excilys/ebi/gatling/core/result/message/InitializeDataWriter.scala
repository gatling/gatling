package com.excilys.ebi.gatling.core.result.message

import java.util.Date

case class InitializeDataWriter(val runOn: Date, val numberOfRelevantActions: Int)