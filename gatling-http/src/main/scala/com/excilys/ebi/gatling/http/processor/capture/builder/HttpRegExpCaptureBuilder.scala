package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpRegExpCaptureBuilder {
	def regexp(expressionFormatter: Context => String) = new HttpRegExpCaptureBuilder(Some(expressionFormatter), None, Some(CompletePageReceived))
	def regexp(expression: String): HttpRegExpCaptureBuilder = regexp((c: Context) => expression)
	def regexp(expressionToFormat: String, interpolations: String*): HttpRegExpCaptureBuilder = regexp((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}
class HttpRegExpCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
		extends AbstractHttpCaptureBuilder[HttpRegExpCaptureBuilder](expressionFormatter, attribute, httpPhase) {

	def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
		new HttpRegExpCaptureBuilder(expressionFormatter, attribute, httpPhase)
	}

	def build: HttpCapture = new HttpRegExpCapture(expressionFormatter.get, attribute.get, httpPhase.get)
}