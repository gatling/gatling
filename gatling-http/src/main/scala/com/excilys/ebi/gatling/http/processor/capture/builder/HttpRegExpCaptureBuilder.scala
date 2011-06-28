package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.builder.HttpCaptureBuilder.HttpCaptureBuilder

import com.excilys.ebi.gatling.http.context._
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.phase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder[HE](expression: Option[String], attribute: Option[String], scope: Option[HttpScope], httpPhase: Option[HttpPhase])
    extends HttpCaptureBuilder[HE](expression, attribute, scope, httpPhase) {

    def inAttribute(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), scope, httpPhase)
    def inRequest(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), Some(new RequestScope), httpPhase)
    def inSession(attrKey: String) = new HttpRegExpCaptureBuilder[HE](expression, Some(attrKey), Some(new SessionScope), httpPhase)
    def onHeaders = new HttpRegExpCaptureBuilder[HE](expression, attribute, scope, Some(new HeadersReceived))
    def onComplete = new HttpRegExpCaptureBuilder[HE](expression, attribute, scope, Some(new CompletePageReceived))
  }

  implicit def enableBuild(builder: HttpRegExpCaptureBuilder[TRUE]) = new {
    def build(): HttpCapture = {
      new HttpRegExpCapture(builder.expression.get, builder.attribute.get, builder.scope.get, builder.httpPhase.get)
    }
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder[TRUE](Some(expression), None, Some(new SessionScope), Some(new CompletePageReceived))

}