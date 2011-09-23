package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

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

  def captureRegexp(expressionFormatter: Context => String) = new HttpRegExpCaptureBuilder(Some(expressionFormatter), None, Some(CompletePageReceived))
  def captureRegexp(expression: String): HttpRegExpCaptureBuilder = captureRegexp((c: Context) => expression)
  def captureRegexp(expressionToFormat: String, interpolations: String*): HttpRegExpCaptureBuilder = captureRegexp((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}