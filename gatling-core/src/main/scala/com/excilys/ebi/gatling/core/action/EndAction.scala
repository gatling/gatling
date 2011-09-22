package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.result.message.ResultStatus._

import java.util.concurrent.CountDownLatch
import java.util.Date

import akka.actor.Actor.registry.actorFor

class EndAction(val latch: CountDownLatch) extends Action {
  def execute(context: Context): Unit = {
    actorFor(context.getWriteActorUuid).map { a =>
      a ! ActionInfo(context.getScenarioName, context.getUserId, "End of scenario", new Date, 0, OK, "End of Scenario Reached")
    }

    latch.countDown

    logger.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
