package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.phase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends HttpCaptureBuilder(expression, attribute, httpPhase) {

    def in(attrKey: String) = new HttpRegExpCaptureBuilder(expression, Some(attrKey), httpPhase)

    def build: HttpCapture = new HttpRegExpCapture(expression.get, attribute.get, httpPhase.get)
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder(Some(expression), None, Some(new CompletePageReceived))

}