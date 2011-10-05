package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpXPathCaptureBuilder {
  def xpath(expressionFormatter: Context => String) = new HttpXPathCaptureBuilder(Some(expressionFormatter), None, Some(CompletePageReceived))
  def xpath(expression: String): HttpXPathCaptureBuilder = xpath((c: Context) => expression)
  def xpath(expressionToFormat: String, interpolations: String*): HttpXPathCaptureBuilder = xpath((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}
class HttpXPathCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
    extends AbstractHttpCaptureBuilder[HttpXPathCaptureBuilder](expressionFormatter, attribute, httpPhase) {

  def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
    new HttpXPathCaptureBuilder(expressionFormatter, attribute, httpPhase)
  }

  def build(): HttpCapture = new HttpXPathCapture(expressionFormatter.get, attribute.get, httpPhase.get)
}
