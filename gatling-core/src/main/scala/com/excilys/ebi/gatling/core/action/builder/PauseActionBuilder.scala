package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.PauseAction
import com.excilys.ebi.gatling.core.action.Action

import java.util.concurrent.TimeUnit

import akka.actor.TypedActor

object PauseActionBuilder {
  class PauseActionBuilder(val delayValue: Option[Int], val delayUnit: Option[TimeUnit], val next: Option[Action]) extends AbstractActionBuilder {
    def withDelayValue(delayValue: Int) = new PauseActionBuilder(Some(delayValue), delayUnit, next)

    def withDelayUnit(delayUnit: TimeUnit) = new PauseActionBuilder(delayValue, Some(delayUnit), next)

    def withNext(next: Action) = new PauseActionBuilder(delayValue, delayUnit, Some(next))

    def build(): Action = {
      logger.debug("Building PauseAction with delay: {}ms", TimeUnit.MILLISECONDS.convert(delayValue.get, delayUnit.get))
      TypedActor.newInstance(classOf[Action], new PauseAction(next.get, delayValue.get, delayUnit.get))
    }
  }

  def pauseActionBuilder = new PauseActionBuilder(None, Some(TimeUnit.SECONDS), None)

}