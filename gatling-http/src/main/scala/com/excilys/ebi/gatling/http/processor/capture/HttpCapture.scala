package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase._

abstract class HttpCapture(val expression: String, val attrKey: String, httpPhase: HttpPhase, val provider: AbstractCaptureProvider)
    extends HttpProcessor(httpPhase) {
  def getAttrKey = attrKey

  def capture(from: Any): Option[Any] = captureInRequest(from, (new String).hashCode.toString)

  def captureInRequest(from: Any, identifier: String): Option[Any]
}
