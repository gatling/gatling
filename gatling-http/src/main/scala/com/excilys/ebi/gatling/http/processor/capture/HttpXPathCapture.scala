package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpPhase

import com.ning.http.client.Response

class HttpXPathCapture(expression: String, attrKey: String, scope: HttpScope, httpPhase: HttpPhase)
  extends HttpCapture(expression, attrKey, scope, httpPhase, new XPathCaptureProvider) {

  def capture(from: Any): Option[Any] = {
    logger.debug("Capturing XPath")
    val placeToSearch =
      if (from.isInstanceOf[Response])
        from.asInstanceOf[Response].getResponseBody
      else
        from
    provider.capture(expression, placeToSearch)
  }

  override def toString = "HttpXPathCapture (" + expression + ")"

}