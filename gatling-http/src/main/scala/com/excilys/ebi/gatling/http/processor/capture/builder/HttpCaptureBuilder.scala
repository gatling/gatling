package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.phase.HttpPhase

abstract class TRUE
abstract class FALSE

object HttpCaptureBuilder {
  abstract class HttpCaptureBuilder(val expression: Option[String], val attribute: Option[String], val httpPhase: Option[HttpPhase])
    extends Logging {

    def in(attrKey: String): HttpCaptureBuilder
  }
}