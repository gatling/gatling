package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.phase.HttpPhase._

object HttpXPathCaptureBuilder {
  class HttpXPathCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends HttpCaptureBuilder(expression, attribute, httpPhase) {

    def in(attrKey: String) = new HttpXPathCaptureBuilder(expression, Some(attrKey), httpPhase)

    def build(): HttpCapture = new HttpXPathCapture(expression.get, attribute.get, httpPhase.get)
  }

  def xpath(expression: String) = new HttpXPathCaptureBuilder(Some(expression), None, Some(CompletePageReceived))

}