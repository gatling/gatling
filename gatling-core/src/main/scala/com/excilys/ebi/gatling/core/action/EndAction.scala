package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.result.message.ActionInfo

import java.util.concurrent.CountDownLatch
import java.util.Date

import akka.actor.Actor.registry.actorFor

class EndAction(val latch: CountDownLatch) extends Action {
  def execute(context: Context): Unit = {
    latch.countDown
    actorFor(context.getWriteActorUuid) match {
      case Some(a) => a ! ActionInfo(context.getUserId, "End of scenario", new Date, 0, "OK")
      case None =>
    }
    logger.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
