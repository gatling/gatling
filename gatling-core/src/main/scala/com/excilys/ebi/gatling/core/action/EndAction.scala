package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context

import java.util.concurrent.CountDownLatch

class EndAction(val latch: CountDownLatch) extends Action {
  def execute(context: Context): Unit = {
    latch.countDown
    logger.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
