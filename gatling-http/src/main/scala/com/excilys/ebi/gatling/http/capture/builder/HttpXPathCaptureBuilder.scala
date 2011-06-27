package com.excilys.ebi.gatling.http.capture.builder

import com.excilys.ebi.gatling.http.capture.builder.HttpCaptureBuilder.HttpCaptureBuilder

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.context.RequestScope
import com.excilys.ebi.gatling.http.context.SessionScope
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.phase._

object HttpXPathCaptureBuilder {
  class HttpXPathCaptureBuilder[HE](expression: Option[String], attribute: Option[String], scope: Option[HttpScope], httpHook: Option[HttpResponseHook])
    extends HttpCaptureBuilder[HE](expression, attribute, scope, httpHook) {

    def inAttribute(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), scope, httpHook)
    def inRequest(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), Some(new RequestScope), httpHook)
    def inSession(attrKey: String) = new HttpXPathCaptureBuilder[HE](expression, Some(attrKey), Some(new SessionScope), httpHook)
  }

  implicit def enableBuild(builder: HttpXPathCaptureBuilder[TRUE]) = new {
    def build(): HttpCapture = new HttpXPathCapture(builder.expression.get, builder.attribute.get, builder.scope.get, builder.httpHook.get)
  }

  def regexp(expression: String) = new HttpXPathCaptureBuilder[TRUE](Some(expression), None, Some(new SessionScope), Some(new CompletePageReceived))

}