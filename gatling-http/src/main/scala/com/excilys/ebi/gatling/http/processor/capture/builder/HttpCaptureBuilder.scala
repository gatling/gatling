package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase._

abstract class HttpCaptureBuilder(val expression: Option[String], val attribute: Option[String], val httpPhase: Option[HttpPhase])
    extends HttpProcessorBuilder {

  def in(attrKey: String): HttpCaptureBuilder

  override def build: HttpCapture
}
