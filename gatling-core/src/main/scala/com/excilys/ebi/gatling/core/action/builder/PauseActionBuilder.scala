package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.PauseAction
import com.excilys.ebi.gatling.core.action.Action

import akka.actor.TypedActor

object PauseActionBuilder {
  class PauseActionBuilder(val delayInMillis: Option[Long], val next: Option[Action]) extends AbstractActionBuilder {
    def withDelay(delayInMillis: Long) = new PauseActionBuilder(Some(delayInMillis), next)

    def withNext(next: Action) = new PauseActionBuilder(delayInMillis, Some(next))

    def build(): Action = {
      logger.debug("Building PauseAction with delay: {}ms and next: {}", delayInMillis.get, next.get)
      TypedActor.newInstance(classOf[Action], new PauseAction(next.get, delayInMillis.get))
    }
  }

  def pauseActionBuilder = new PauseActionBuilder(None, None)

}