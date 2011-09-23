package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context

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

  def header(expression: String) = new HttpHeaderCaptureBuilder(Some((c: Context) => expression), None, None)
}