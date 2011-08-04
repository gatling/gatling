package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture

object HttpHeaderCaptureBuilder {
  class HttpHeaderCaptureBuilder(expression: Option[String], attribute: Option[String])
      extends HttpCaptureBuilder(expression, attribute, null) {

    def in(attrKey: String) = new HttpHeaderCaptureBuilder(expression, Some(attrKey))

    def build: HttpCapture = new HttpHeaderCapture(expression.get, attribute.get)
  }

  def header(expression: String) = new HttpHeaderCaptureBuilder(Some(expression), None)
}