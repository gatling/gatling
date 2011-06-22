package com.excilys.ebi.gatling.core.action

import akka.actor.Scheduler
import akka.actor.TypedActor
import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.ElapsedActionTime
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder

class PauseAction(next: Action, delayInMillis: Long) extends AbstractAction {
  def execute(context: Context) = {

    val delayInNanos: Long = delayInMillis * 1000000 - (if (context.isInstanceOf[ElapsedActionTime]) context.asInstanceOf[ElapsedActionTime].getElapsedActionTime else 0L)
//    println("Waiting for " + delayInMillis + "ms")
    Scheduler.scheduleOnce(() => next.execute(context), delayInNanos, TimeUnit.NANOSECONDS)

    if (context.getUserId == 0) {
      self.stop
    }
  }
}