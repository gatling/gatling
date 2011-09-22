package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture

import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpHeaderCaptureBuilder {
  class HttpHeaderCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpHeaderCaptureBuilder](expression, attribute, httpPhase) {

    def newInstance(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpHeaderCaptureBuilder(expression, attribute, httpPhase)
    }

    def build: HttpCapture = new HttpHeaderCapture(expression.get, attribute.get)
  }

  def header(expression: String) = new HttpHeaderCaptureBuilder(Some(expression), None, None)
}