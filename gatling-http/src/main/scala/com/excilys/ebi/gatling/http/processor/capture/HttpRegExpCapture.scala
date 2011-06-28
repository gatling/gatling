package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpPhase

import scala.util.matching.Regex

import com.ning.http.client.Response

class HttpRegExpCapture(expression: String, attrKey: String, scope: HttpScope, httpPhase: HttpPhase)
  extends HttpCapture(expression, attrKey, scope, httpPhase, new RegExpCaptureProvider) {

  def capture(from: Any): Option[Any] = {
    logger.debug("Capturing RegExp...")
    val placeToSearch =
      if (from.isInstanceOf[Response])
        from.asInstanceOf[Response].getResponseBody
      else
        from

    provider.capture(expression, placeToSearch)
  }

  override def toString = "HttpRegExpCapture (" + expression + ")"

}