package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture

import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpHeaderCaptureBuilder {
  class HttpHeaderCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpHeaderCaptureBuilder](expressionFormatter, attribute, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpHeaderCaptureBuilder(expressionFormatter, attribute, httpPhase)
    }

    def build: HttpCapture = new HttpHeaderCapture(expressionFormatter.get, attribute.get)
  }

  def captureHeader(expressionFormatter: Context => String) = new HttpHeaderCaptureBuilder(Some(expressionFormatter), None, None)
  def captureHeader(expression: String): HttpHeaderCaptureBuilder = captureHeader((c: Context) => expression)
  def captureHeader(expressionToFormat: String, interpolations: String*): HttpHeaderCaptureBuilder = captureHeader((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}