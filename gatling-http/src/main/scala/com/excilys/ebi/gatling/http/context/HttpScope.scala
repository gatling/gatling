package com.excilys.ebi.gatling.http.context

import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder.HttpContextBuilder
import com.excilys.ebi.gatling.http.context.builder.TRUE

sealed trait HttpScope {
  def setAttribute(builder: HttpContextBuilder[TRUE], attrKey: String, attrValue: Any): HttpContextBuilder[TRUE]
}
case class RequestScope extends HttpScope {
  def setAttribute(builder: HttpContextBuilder[TRUE], attrKey: String, attrValue: Any): HttpContextBuilder[TRUE] = {
    builder setRequestAttribute (attrKey -> attrValue)
  }

  override def equals(that: Any): Boolean = {
    that.isInstanceOf[RequestScope]
  }

}
case class SessionScope extends HttpScope {
  def setAttribute(builder: HttpContextBuilder[TRUE], attrKey: String, attrValue: Any): HttpContextBuilder[TRUE] = {
    builder setSessionAttribute (attrKey -> attrValue)
  }

  override def equals(that: Any): Boolean = {
    that.isInstanceOf[SessionScope]
  }
}
