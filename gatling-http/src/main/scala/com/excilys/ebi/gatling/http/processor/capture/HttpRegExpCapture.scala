package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

import scala.util.matching.Regex

import com.ning.http.client.Response

class HttpRegExpCapture(expression: String, attrKey: String, httpPhase: HttpPhase)
  extends HttpCapture(expression, attrKey, httpPhase, new RegExpCaptureProvider) {

  def capture(from: Any): Option[Any] = {
    logger.debug("Capturing RegExp...")
    val placeToSearch =
      from match {
        case r: Response => r.getResponseBody
        case _ => throw new IllegalArgumentException
      }
    provider.capture(expression, placeToSearch)
  }

  override def toString = "HttpRegExpCapture (" + expression + ")"

}