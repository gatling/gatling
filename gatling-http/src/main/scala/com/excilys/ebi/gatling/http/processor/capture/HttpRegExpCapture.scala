package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpPhase
import scala.util.matching.Regex

class HttpRegExpCapture(expression: String, attrKey: String, scope: HttpScope, httpPhase: HttpPhase)
  extends HttpCapture(expression, attrKey, scope, httpPhase, new RegExpCaptureProvider) {

  override def toString = "HttpRegExpCapture (" + expression + ")"

}