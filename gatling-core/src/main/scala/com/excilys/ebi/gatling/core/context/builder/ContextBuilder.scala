package com.excilys.ebi.gatling.core.context.builder

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.context.Context
import akka.actor.Uuid
import com.ning.http.client.Cookie
import scala.collection.mutable.HashMap

abstract class TRUE

object ContextBuilder {
  class ContextBuilder[HSN, HUID, HWAU](val scenarioName: Option[String], val userId: Option[Int], val writeActorUuid: Option[Uuid], val cookies: Option[List[Cookie]], val data: Option[Map[String, String]])
      extends Logging {

    def fromContext(context: Context) = new ContextBuilder[TRUE, TRUE, TRUE](Some(context.getScenarioName), Some(context.getUserId), Some(context.getWriteActorUuid), Some(context.getCookies), Some(context.getData))

    def withUserId(userId: Int) = new ContextBuilder[HSN, TRUE, HWAU](scenarioName, Some(userId), writeActorUuid, cookies, data)

    def withWriteActorUuid(writeActorUuid: Uuid) = new ContextBuilder[HSN, HUID, TRUE](scenarioName, userId, Some(writeActorUuid), cookies, data)

    def withScenarioName(scenarioName: String) = new ContextBuilder[TRUE, HUID, HWAU](Some(scenarioName), userId, writeActorUuid, cookies, data)

    def withData(data: Map[String, String]) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data))

    def setAttribute(attr: Tuple2[String, String]) = {
      logger.debug("Setting '{}'='{}'", attr._1, attr._2)
      new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data.get + (attr._1 -> attr._2)))
    }

    def unsetAttribute(attrKey: String) = new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, cookies, Some(data.get - attrKey))

    def getAttribute(attrKey: String) = data.get.get(attrKey)

    def setDuration(value: Long) = unsetAttribute(Context.LAST_ACTION_DURATION_ATTR_NAME) setAttribute (Context.LAST_ACTION_DURATION_ATTR_NAME, value.toString)

    def setCookies(cookies: java.util.List[Cookie]) = {
      var cookiesMap: HashMap[String, Cookie] = new HashMap
      for (cookie <- this.cookies.getOrElse(Nil))
        cookiesMap += (cookie.getName -> cookie)

      val it = cookies.iterator
      while (it.hasNext) {
        val cookie = it.next
        cookiesMap += (cookie.getName -> cookie)
      }

      var cookiesList: List[Cookie] = Nil
      for (c <- cookiesMap)
        cookiesList = c._2 :: cookiesList

      new ContextBuilder[HSN, HUID, HWAU](scenarioName, userId, writeActorUuid, Some(cookiesList), data)
    }
  }

  implicit def enableBuild(builder: ContextBuilder[TRUE, TRUE, TRUE]) = new {
    def build(): Context = {
      val context = new Context(builder.scenarioName.get, builder.userId.get, builder.writeActorUuid.get, builder.cookies.get, builder.data.get)
      builder.logger.debug("Built Context")
      context
    }
  }

  def newContext = new ContextBuilder(None, None, None, Some(List()), Some(Map()))
}