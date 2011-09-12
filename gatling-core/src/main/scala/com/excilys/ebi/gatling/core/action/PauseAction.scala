package com.excilys.ebi.gatling.core.action

import akka.actor.Scheduler
import akka.actor.TypedActor
import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

class PauseAction(next: Action, delayValue: Int, delayUnit: TimeUnit) extends Action {
  def execute(context: Context) = {
    val delayInNanos: Long = TimeUnit.NANOSECONDS.convert(delayValue, delayUnit) - context.getElapsedActionTime

    logger.info("Waiting for {}ms ({}ms)", TimeUnit.MILLISECONDS.convert(delayValue, delayUnit), TimeUnit.MILLISECONDS.convert(delayInNanos, TimeUnit.NANOSECONDS))

    Scheduler.scheduleOnce(() => next.execute(context), delayInNanos, TimeUnit.NANOSECONDS)
  }

  override def toString = "Pause Action"
}
