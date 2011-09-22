package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase._

abstract class AbstractHttpCaptureBuilder[B <: AbstractHttpCaptureBuilder[B]](val expression: Option[String], val attribute: Option[String], val httpPhase: Option[HttpPhase])
    extends HttpProcessorBuilder {

  def newInstance(expression: Option[String], attribute: Option[String], httpPhase: Option[HttpPhase]): B

  def in(attrKey: String): B = {
    newInstance(expression, Some(attrKey), httpPhase)
  }

  override def build: HttpCapture
}
