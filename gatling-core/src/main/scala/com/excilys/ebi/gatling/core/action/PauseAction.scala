package com.excilys.ebi.gatling.core.action

import akka.actor.Scheduler
import akka.actor.TypedActor
import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

class PauseAction(next: Action, duration: Long, timeUnit: TimeUnit) extends Action {
  def execute(context: Context) = {
    val durationInNanos: Long = TimeUnit.NANOSECONDS.convert(duration, timeUnit) - context.getLastActionDuration

    logger.info("Waiting for {}ms ({}ms)", TimeUnit.MILLISECONDS.convert(duration, timeUnit), TimeUnit.MILLISECONDS.convert(durationInNanos, TimeUnit.NANOSECONDS))

    Scheduler.scheduleOnce(() => next.execute(context), durationInNanos, TimeUnit.NANOSECONDS)
  }

  override def toString = "Pause Action"
}
