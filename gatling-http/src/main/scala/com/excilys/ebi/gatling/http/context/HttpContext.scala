package com.excilys.ebi.gatling.http.context

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.ElapsedActionTime

import akka.actor.Uuid

class HttpContext(givenUserId: Int, writeActorUuid: Uuid, session: Map[String, Any], request: Map[String, Any])
  extends Context(givenUserId, writeActorUuid) with ElapsedActionTime {

  def getSession = session

  def getRequest = request

  def getElapsedActionTime =
    session.get("elapsedTime").map { l =>
      l.asInstanceOf[Long]
    }.getOrElse(0L)
}