package com.excilys.ebi.gatling.http.context

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.ElapsedActionTime

class HttpContext(givenUserId: Integer, session: Map[String, Any], request: Map[String, Any]) extends Context(givenUserId) with ElapsedActionTime {

  def getSession = session

  def getRequest = request

  def getElapsedActionTime =
    session.get("elapsedTime") match {
      case Some(l) => l.asInstanceOf[Long]
      case None => 0L
    }
}