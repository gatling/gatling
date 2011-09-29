package com.excilys.ebi.gatling.core.result.message

import java.util.concurrent.CountDownLatch
import org.joda.time.DateTime

case class InitializeDataWriter(val runOn: DateTime, val numberOfRelevantActions: Int, val latch: CountDownLatch)