package com.excilys.ebi.gatling.http.capture.builder

import com.excilys.ebi.gatling.http.capture.builder.HttpCaptureBuilder.HttpCaptureBuilder

import com.excilys.ebi.gatling.http.context._
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.phase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder[HE](expression: Option[String], attribute: Option[String], scope: Option[HttpScope], httpHook: Option[HttpResponseHook])
    extends HttpCaptureBuilder[HE](expression, attribute, scope, httpHook) {

    def inAttribute(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), scope, httpHook)
    def inRequest(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), Some(new RequestScope), httpHook)
    def inSession(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), Some(new SessionScope), httpHook)
    def onHeaders = new HttpRegExpCaptureBuilder[HE](expression, attribute, scope, Some(new HeadersReceived))
    def onComplete = new HttpRegExpCaptureBuilder[HE](expression, attribute, scope, Some(new CompletePageReceived))
  }

  implicit def enableBuild(builder: HttpRegExpCaptureBuilder[TRUE]) = new {
    def build(): HttpCapture = {
      new HttpRegExpCapture(builder.expression.get, builder.attribute.get, builder.scope.get, builder.httpHook.get)
    }
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder[TRUE](Some(expression), None, Some(new SessionScope), Some(new CompletePageReceived))

}