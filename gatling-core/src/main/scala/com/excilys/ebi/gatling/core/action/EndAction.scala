package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.statistics.ActionInfo

import java.util.concurrent.CountDownLatch

import akka.actor.Actor.registry.actorFor

class EndAction(val latch: CountDownLatch) extends Action {
  def execute(context: Context): Unit = {
    latch.countDown
    actorFor(context.getWriteActorUuid) match {
      case Some(a) => a ! ActionInfo("Default", context.getUserId, "End Of Scenario", 0)
      case None =>
    }
    logger.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
