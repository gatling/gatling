package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid
import com.excilys.ebi.gatling.core.log.Logging
import com.ning.http.client.Cookie
import org.apache.commons.lang3.StringUtils

object Context {
  val LAST_ACTION_DURATION_ATTR_NAME = "gatling.lastActionDuration"
}
class Context(val scenarioName: String, val userId: Int, val writeActorUuid: Uuid, val cookies: List[Cookie], var data: Map[String, String]) extends Logging {

  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
  def getCookies = cookies
  def getScenarioName = scenarioName

  def getData = data

  def getAttribute(key: String): String = {
    val result = data.get(key).getOrElse {
      logger.warn("No Matching Attribute for key: '{}' in context, setting to ''", key)
      StringUtils.EMPTY
    }
    logger.debug("Context('{}') = {}", key, result)
    result
  }

  def setAttributes(attributes: Map[String, String]) = {
    data ++= attributes
  }

  def getLastActionDuration: Long =
    data.get(Context.LAST_ACTION_DURATION_ATTR_NAME).map { l =>
      l.toLong
    }.getOrElse(0L)
}
