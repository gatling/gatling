package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.EndAction

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.actor.TypedActor

object EndActionBuilder {
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[EndAction]);

  class EndActionBuilder extends AbstractActionBuilder {

    def build(): Action = {
      LOGGER.debug("Building EndAction")
      TypedActor.newInstance(classOf[Action], classOf[EndAction])
    }

    def withNext(next: Action): AbstractActionBuilder = this

    override def toString = "End"
  }

  def endActionBuilder = new EndActionBuilder
}