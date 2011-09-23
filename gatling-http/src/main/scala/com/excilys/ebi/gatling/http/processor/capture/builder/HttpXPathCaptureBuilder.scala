package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpXPathCaptureBuilder {
  class HttpXPathCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
      extends AbstractHttpCaptureBuilder[HttpXPathCaptureBuilder](expressionFormatter, attribute, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpXPathCaptureBuilder(expressionFormatter, attribute, httpPhase)
    }

    def build(): HttpCapture = new HttpXPathCapture(expressionFormatter.get, attribute.get, httpPhase.get)
  }

  def xpath(expression: String) = new HttpXPathCaptureBuilder(Some((c: Context) => expression), None, Some(CompletePageReceived))

}