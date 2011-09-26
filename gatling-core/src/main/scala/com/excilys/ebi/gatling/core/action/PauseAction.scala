package com.excilys.ebi.gatling.core.action

import akka.actor.Scheduler

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.context.Context

import scala.util.Random

class PauseAction(next: Action, minDuration: Long, maxDuration: Long, timeUnit: TimeUnit) extends Action {
  val randomGenerator = new Random

  def execute(context: Context) = {

    val duration = if (maxDuration - minDuration > 0)
      randomGenerator.nextInt((maxDuration - minDuration).toInt) + minDuration
    else
      minDuration

    val durationInNanos: Long = TimeUnit.NANOSECONDS.convert(duration, timeUnit) - context.getLastActionDuration

    logger.info("Waiting for {}ms ({}ms)", TimeUnit.MILLISECONDS.convert(duration, timeUnit), TimeUnit.MILLISECONDS.convert(durationInNanos, TimeUnit.NANOSECONDS))

    Scheduler.scheduleOnce(() => next.execute(context), durationInNanos, TimeUnit.NANOSECONDS)
  }

  override def toString = "Pause Action"
}
