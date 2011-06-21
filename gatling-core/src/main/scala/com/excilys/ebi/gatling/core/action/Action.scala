package com.excilys.ebi.gatling.core.action

import akka.actor.TypedActor

import com.excilys.ebi.gatling.core.context.Context

trait Action extends TypedActor {

  def execute(context: Context)
}