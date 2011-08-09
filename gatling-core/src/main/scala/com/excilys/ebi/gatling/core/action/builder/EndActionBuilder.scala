package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.EndAction

import akka.actor.TypedActor

import java.util.concurrent.CountDownLatch

object EndActionBuilder {
  class EndActionBuilder(val latch: CountDownLatch) extends AbstractActionBuilder {

    def build(scenarioId: Int): Action = {
      logger.debug("Building EndAction of scenario {}", scenarioId)
      TypedActor.newInstance(classOf[Action], new EndAction(latch))
    }

    def withNext(next: Action): AbstractActionBuilder = this

    override def toString = "End"
  }

  def endActionBuilder(latch: CountDownLatch) = new EndActionBuilder(latch)
}