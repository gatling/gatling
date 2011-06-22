package com.excilys.ebi.gatling.http.context.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.context.HttpContext

abstract class TRUE

object HttpContextBuilder {
  class HttpContextBuilder[HUID](val userId: Option[Integer], val session: Option[Map[String, Any]], val request: Option[Map[String, Any]]) {

    def fromContext(context: HttpContext) = new HttpContextBuilder[TRUE](Some(context.getUserId), Some(context.getSession), Some(Map.empty[String, String]))

    def withUserId(userId: Integer) = new HttpContextBuilder[TRUE](Some(userId), session, request)

    def withSessionScope(session: Map[String, Any]) = new HttpContextBuilder[HUID](userId, Some(session), request)

    def withRequestScope(request: Map[String, Any]) = new HttpContextBuilder[HUID](userId, session, Some(request))

    def setSessionAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID](userId, Some(session.get + (attr._1 -> attr._2)), request)

    def unsetSessionAttribute(attrKey: String) = new HttpContextBuilder[HUID](userId, Some(session.get - attrKey), request)

    def getSessionAttribute(attrKey: String) = session.get.get(attrKey)

    def setRequestAttribute(attr: Tuple2[String, Any]) = new HttpContextBuilder[HUID](userId, session, Some(request.get + (attr._1 -> attr._2)))

    def unsetRequestAttribute(attrKey: String) = new HttpContextBuilder[HUID](userId, session, Some(request.get - attrKey))

    def getRequestAttribute(attrKey: String) = request.get.get(attrKey)

  }

  implicit def enableBuild(builder: HttpContextBuilder[TRUE]) = new {
    def build(): HttpContext = {
      new HttpContext(builder.userId.get, builder.session.get, builder.request.get)
    }
  }

  def httpContext = new HttpContextBuilder(None, Some(Map()), Some(Map()))
}