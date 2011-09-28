package com.excilys.ebi.gatling.core.result.message

import java.util.Date
import java.util.concurrent.CountDownLatch

case class InitializeDataWriter(val runOn: Date, val numberOfRelevantActions: Int, val latch: CountDownLatch)