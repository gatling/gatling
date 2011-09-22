package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpXPathCaptureBuilder {
  class HttpXPathCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpXPathCaptureBuilder](expression, attribute, httpPhase) {

    def newInstance(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpXPathCaptureBuilder(expression, attribute, httpPhase)
    }

    def build(): HttpCapture = new HttpXPathCapture(expression.get, attribute.get, httpPhase.get)
  }

  def xpath(expression: String) = new HttpXPathCaptureBuilder(Some(expression), None, Some(CompletePageReceived))

}