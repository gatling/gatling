package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid
import com.excilys.ebi.gatling.core.log.Logging
import com.ning.http.client.Cookie

class Context(val userId: Int, val writeActorUuid: Uuid, val cookies: List[Cookie], var data: Map[String, String]) extends ElapsedActionTime with Logging {
  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
  def getCookies = cookies

  def getData = data

  def getAttribute(key: String): String = {
    val result = data.get(key).getOrElse(throw new Exception("No matching attribute"))
    logger.debug("Context('{}') = {}", key, result)
    result
  }

  def setAttributes(attributes: Map[String, String]) = {
    data ++= attributes
  }

  def getElapsedActionTime: Long =
    data.get("gatlingElapsedTime").map { l =>
      l.toLong
    }.getOrElse(0L)
}