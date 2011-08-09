package com.excilys.ebi.gatling.core.context.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context

import akka.actor.Uuid

import com.ning.http.client.Cookie

abstract class TRUE

object ContextBuilder {
  class ContextBuilder[HSN, HUID, HWAU](val scenarioName: Option[String], val userId: Option[Int], val writeActorUuid: Option[Uuid], val data: Option[Map[String, String]], val cookies: Option[List[Cookie]])
      extends Logging {

    def fromContext(context: Context) = new ContextBuilder[TRUE, TRUE, TRUE](Some(context.getScenarioName), Some(context.getUserId), Some(context.getWriteActorUuid), Some(context.getData), Some(context.getCookies))

    def withUserId(userId: Int) = new ContextBuilder[HSN, TRUE, HWAU](scenarioName, Some(userId), writeActorUuid, data, cookies)

    def withWriteActorUuid(writeActorUuid: Uuid) = new ContextBuilder[HSN, HUID, TRUE](scenarioName, userId, Some(writeActorUuid), data, cookies)

    def withScenarioName(scenarioName: String) = new ContextBuilder[TRUE, HUID, HWAU](Some(scenarioName), userId, writeActorUuid, data, cookies)

    def withData(data: Map[String, String]) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, Some(data), cookies)

    def setAttribute(attr: Tuple2[String, String]) = {
      logger.debug("Setting '{}' in '{}'", attr._2, attr._1)
      new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, Some(data.get + (attr._1 -> attr._2)), cookies)
    }

    def unsetAttribute(attrKey: String) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, Some(data.get - attrKey), cookies)

    def getAttribute(attrKey: String) = data.get.get(attrKey)

    def setElapsedActionTime(value: Long) = unsetAttribute("gatlingElapsedTime") setAttribute ("gatlingElapsedTime", value.toString)

    def setCookies(cookies: java.util.List[Cookie]) = {
      var cookiesList: List[Cookie] = Nil
      val it = cookies.iterator
      while (it.hasNext) {
        cookiesList = it.next :: cookiesList
      }
      new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, data, Some(cookiesList))
    }
  }

  implicit def enableBuild(builder: ContextBuilder[TRUE, TRUE, TRUE]) = new {
    def build(): Context = {
      val context = new Context(builder.scenarioName.get, builder.userId.get, builder.writeActorUuid.get, builder.cookies.get, builder.data.get)
      builder.logger.debug("Built Context")
      context
    }
  }

  def makeContext = new ContextBuilder(None, None, None, Some(Map()), Some(List()))
}