package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase._

abstract class HttpCapture(val expression: String, val attrKey: String, val scope: HttpScope, httpPhase: HttpPhase, val provider: AbstractCaptureProvider)
  extends HttpProcessor(httpPhase) {
  def getScope = scope
  def getAttrKey = attrKey

  def capture(from: Any): Option[Any] = {
    logger.debug("Capturing...")
    provider.capture(expression, from)
  }
}
