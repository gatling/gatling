package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid
import com.excilys.ebi.gatling.core.log.Logging
import com.ning.http.client.Cookie
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.TimeUnit

object Context {
  val LAST_ACTION_DURATION_ATTR_NAME = "gatling.lastActionDuration"
  val WHILE_DURATION = "gatling.whileDurationElapsed"
  val LAST_WHILE_ACCESS = "gatling.lastWhileAccess"
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

  def getWhileDuration: Long = {
    val currentValue = data.get(Context.WHILE_DURATION).map { l =>
      l.toLong
    }.getOrElse(0L)
    logger.debug("Current Duration Value: {}", currentValue)

    val nowMillis =
      System.currentTimeMillis

    if (currentValue == 0) {
      setLastWhileAccess(nowMillis)
      setWhileDuration(1L)
      1L
    } else {
      val newValue = currentValue + nowMillis - getLastWhileAccess
      setWhileDuration(newValue)
      setLastWhileAccess(nowMillis)
      logger.debug("New Duration Value: {}", newValue)
      newValue
    }

  }

  def resetWhileDuration = setWhileDuration(0L)

  private def setWhileDuration(value: Long) = {
    data += (Context.WHILE_DURATION -> value.toString)
  }

  private def setLastWhileAccess(value: Long) = {
    data += (Context.LAST_WHILE_ACCESS -> value.toString)
  }

  private def getLastWhileAccess = {
    data.get(Context.LAST_WHILE_ACCESS).map {
      l => l.toLong
    }.getOrElse(0L)
  }
}
