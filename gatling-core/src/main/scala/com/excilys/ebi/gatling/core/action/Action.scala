package com.excilys.ebi.gatling.core.action

import akka.actor.TypedActor

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging

trait Action extends TypedActor with Logging {
  def execute(context: Context)
}