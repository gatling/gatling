package com.excilys.ebi.gatling.http.capture

import com.excilys.ebi.gatling.core.capture.AbstractCapture
import com.excilys.ebi.gatling.core.capture.provider.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase._

abstract class HttpCapture(val expression: String, val attrKey: String, val scope: HttpScope, httpHook: HttpResponseHook, val provider: AbstractCaptureProvider)
  extends HttpProcessor(httpHook) with AbstractCapture {
  def getScope = scope
  def getAttrKey = attrKey

  def capture(from: Any): Option[Any] = {
    logger.debug("Capturing...")
    provider.capture(expression, from)
  }
}
