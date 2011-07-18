package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.http.processor.capture.builder.HttpCaptureBuilder.HttpCaptureBuilder

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.phase._

object HttpRegExpCaptureBuilder {
  class HttpRegExpCaptureBuilder(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase])
    extends HttpCaptureBuilder(expression, attribute, httpPhase) {

    def in(attrKey: String): HttpRegExpCaptureBuilder = new HttpRegExpCaptureBuilder(expression, Some(attrKey), httpPhase)
    def onHeaders = new HttpRegExpCaptureBuilder(expression, attribute, Some(new HeadersReceived))
    def onComplete = new HttpRegExpCaptureBuilder(expression, attribute, Some(new CompletePageReceived))

    def build: HttpCapture = new HttpRegExpCapture(expression.get, attribute.get, httpPhase.get)
  }

  def regexp(expression: String) = new HttpRegExpCaptureBuilder(Some(expression), None, Some(new CompletePageReceived))

}