package com.excilys.ebi.gatling.core.action

import akka.actor.TypedActor

import com.excilys.ebi.gatling.core.context.Context

abstract class AbstractAction extends TypedActor {
  def execute(context: Context)
}