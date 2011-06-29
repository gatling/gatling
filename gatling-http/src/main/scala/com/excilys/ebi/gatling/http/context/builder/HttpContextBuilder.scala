package com.excilys.ebi.gatling.http.context.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.http.context.HttpContext

import akka.actor.Uuid

abstract class TRUE

object HttpContextBuilder {
  class HttpContextBuilder[HUID, HWAU](val userId: Option[Int], val writeActorUuid: Option[Uuid], val session: Option[Map[String, Any]], val request: Option[Map[String, Any]]) extends Logging {

    def fromContext(context: HttpContext) = new HttpContextBuilder[TRUE, TRUE](Some(context.getUserId), Some(context.getWriteActorUuid), Some(context.getSession), Some(Map.empty[String, String]))

    def withUserId(userId: Int) = new HttpContextBuilder[TRUE, HWAU](Some(userId), writeActorUuid, session, request)

    def withWriteActorUuid(writeActorUuid: Uuid) = new HttpContextBuilder[HUID, TRUE](userId, Some(writeActorUuid), session, request)

    def withSessionScope(session: Map[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session), request)

    def withRequestScope(request: Map[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request))

    def setSessionAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session.get + (attr._1 -> attr._2)), request)

    def unsetSessionAttribute(attrKey: String) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session.get - attrKey), request)

    def getSessionAttribute(attrKey: String) = session.get.get(attrKey)

    def setRequestAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request.get + (attr._1 -> attr._2)))

    def unsetRequestAttribute(attrKey: String) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request.get - attrKey))

    def getRequestAttribute(attrKey: String) = request.get.get(attrKey)

    def setElapsedActionTime(value: Long) = unsetSessionAttribute("elapsedTime") setSessionAttribute ("elapsedTime", value)
  }

  implicit def enableBuild(builder: HttpContextBuilder[TRUE, TRUE]) = new {
    def build(): HttpContext = {
      val context = new HttpContext(builder.userId.get, builder.writeActorUuid.get, builder.session.get, builder.request.get)
      builder.logger.debug("Built HttpContext")
      context
    }
  }

  def httpContext = new HttpContextBuilder(None, None, Some(Map()), Some(Map()))
}