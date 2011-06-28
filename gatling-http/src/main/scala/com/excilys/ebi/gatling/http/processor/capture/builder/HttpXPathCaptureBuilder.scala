package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.builder.HttpCaptureBuilder.HttpCaptureBuilder

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.context.RequestScope
import com.excilys.ebi.gatling.http.context.SessionScope
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.phase._

object HttpXPathCaptureBuilder {
  class HttpXPathCaptureBuilder[HE](expression: Option[String], attribute: Option[String], scope: Option[HttpScope], httpPhase: Option[HttpPhase])
    extends HttpCaptureBuilder[HE](expression, attribute, scope, httpPhase) {

    def inAttribute(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), scope, httpPhase)
    def inRequest(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), Some(new RequestScope), httpPhase)
    def inSession(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), Some(new SessionScope), httpPhase)
  }

  implicit def enableBuild(builder: HttpXPathCaptureBuilder[TRUE]) = new {
    def build(): HttpCapture = new HttpXPathCapture(builder.expression.get, builder.attribute.get, builder.scope.get, builder.httpPhase.get)
  }

  def xpath(expression: String) = new HttpXPathCaptureBuilder[TRUE](Some(expression), None, Some(new SessionScope), Some(new CompletePageReceived))

}