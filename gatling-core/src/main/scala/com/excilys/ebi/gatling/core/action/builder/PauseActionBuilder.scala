package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.PauseAction
import com.excilys.ebi.gatling.core.action.AbstractAction

import akka.actor.TypedActor

object PauseActionBuilder {
  class PauseActionBuilder(val delayInMillis: Option[Long], val next: Option[AbstractActionBuilder]) extends AbstractActionBuilder {
    def withDelay(delayInMillis: Long) = new PauseActionBuilder(Some(delayInMillis), next)

    def withNext(next: AbstractActionBuilder) = new PauseActionBuilder(delayInMillis, Some(next))

    def build(): AbstractAction =
      TypedActor.newInstance(classOf[AbstractAction], () => new PauseAction(next.get.build, delayInMillis.get))
  }

  def pauseActionBuilder = new PauseActionBuilder(None, None)

}