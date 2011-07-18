package com.excilys.ebi.gatling.http.context.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.http.context.HttpContext

import akka.actor.Uuid

abstract class TRUE

object HttpContextBuilder {
  class HttpContextBuilder[HUID, HWAU](val userId: Option[Int], val writeActorUuid: Option[Uuid], val session: Option[Map[String, Any]], val request: Option[Map[String, Any]], val feederIndex: Option[Int])
    extends Logging {

    def fromContext(context: HttpContext) = new HttpContextBuilder[TRUE, TRUE](Some(context.getUserId), Some(context.getWriteActorUuid), Some(context.getSession), Some(Map.empty[String, String]), Some(context.getFeederIndex))

    def withUserId(userId: Int) = new HttpContextBuilder[TRUE, HWAU](Some(userId), writeActorUuid, session, request, feederIndex)

    def withWriteActorUuid(writeActorUuid: Uuid) = new HttpContextBuilder[HUID, TRUE](userId, Some(writeActorUuid), session, request, feederIndex)

    def withSessionScope(session: Map[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session), request, feederIndex)

    def withRequestScope(request: Map[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request), feederIndex)

    def setSessionAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session.get + (attr._1 -> attr._2)), request, feederIndex)

    def unsetSessionAttribute(attrKey: String) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, Some(session.get - attrKey), request, feederIndex)

    def getSessionAttribute(attrKey: String) = session.get.get(attrKey)

    def setRequestAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request.get + (attr._1 -> attr._2)), feederIndex)

    def unsetRequestAttribute(attrKey: String) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, Some(request.get - attrKey), feederIndex)

    def getRequestAttribute(attrKey: String) = request.get.get(attrKey)

    def setElapsedActionTime(value: Long) = unsetSessionAttribute("elapsedTime") setSessionAttribute ("elapsedTime", value)

    def withFeederIndex(feederIndex: Int) = new HttpContextBuilder[HUID, HWAU](userId, writeActorUuid, session, request, Some(feederIndex))
  }

  implicit def enableBuild(builder: HttpContextBuilder[TRUE, TRUE]) = new {
    def build(): HttpContext = {
      val context = new HttpContext(builder.userId.get, builder.writeActorUuid.get, builder.feederIndex.getOrElse(-1), builder.session.get, builder.request.get)
      builder.logger.debug("Built HttpContext")
      context
    }
  }

  def httpContext = new HttpContextBuilder(None, None, Some(Map()), Some(Map()), None)
}