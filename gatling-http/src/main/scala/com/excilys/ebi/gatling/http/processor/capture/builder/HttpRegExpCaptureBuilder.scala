package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpRegExpCaptureBuilder](expressionFormatter, attribute, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpRegExpCaptureBuilder(expressionFormatter, attribute, httpPhase)
    }

    def build: HttpCapture = new HttpRegExpCapture(expressionFormatter.get, attribute.get, httpPhase.get)
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder(Some((c: Context) => expression), None, Some(CompletePageReceived))
}