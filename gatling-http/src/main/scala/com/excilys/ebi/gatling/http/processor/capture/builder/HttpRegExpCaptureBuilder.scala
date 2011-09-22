package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpRegExpCaptureBuilder](expression, attribute, httpPhase) {

    def newInstance(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpRegExpCaptureBuilder(expression, attribute, httpPhase)
    }

    def build: HttpCapture = new HttpRegExpCapture(expression.get, attribute.get, httpPhase.get)
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder(Some(expression), None, Some(CompletePageReceived))
}