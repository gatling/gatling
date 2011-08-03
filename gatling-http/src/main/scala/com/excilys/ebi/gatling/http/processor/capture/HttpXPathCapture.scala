package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

import com.ning.http.client.Response

class HttpXPathCapture(expression: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpCapture(expression, attrKey, httpPhase, null) {

  def captureInRequest(from: Any, identifier: String): Option[Any] = {
    logger.debug("Capturing XPath...")
    val response =
      from match {
        case r: Response => r
        case _ => throw new IllegalArgumentException
      }
    XPathCaptureProvider.getInstance(identifier).capture(expression, response.getResponseBodyAsBytes)
  }

  override def toString = "HttpXPathCapture (" + expression + ")"

}