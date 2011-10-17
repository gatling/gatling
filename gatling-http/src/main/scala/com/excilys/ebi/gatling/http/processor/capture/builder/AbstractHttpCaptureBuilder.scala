package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase._

abstract class AbstractHttpCaptureBuilder[B <: AbstractHttpCaptureBuilder[B]](val expressionFormatter: Option[Context => String], val attribute: Option[String], val httpPhase: Option[HttpPhase])
		extends HttpProcessorBuilder {

	def newInstance(expression: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]): B

	def in(attrKey: String): B = {
		newInstance(expressionFormatter, Some(attrKey), httpPhase)
	}

	override def build: HttpCapture
}
