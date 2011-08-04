package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.http.phase.HeadersReceived
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider

class HttpHeaderCapture(expression: String, attrKey: String)
    extends HttpCapture(expression, attrKey, new HeadersReceived, new HttpHeadersCaptureProvider) {

  def captureInRequest(from: Any, identifier: String): Option[Any] = {
    logger.debug("Capturing Header...")
    provider.capture(expression, from)
  }

  override def toString = "HttpHeaderCapture (" + expression + ")"
}