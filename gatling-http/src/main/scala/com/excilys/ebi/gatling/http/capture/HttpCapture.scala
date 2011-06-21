package com.excilys.ebi.gatling.http.capture

import com.excilys.ebi.gatling.core.capture.AbstractCapture
import com.excilys.ebi.gatling.core.capture.provider.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase._

abstract class HttpCapture(val expression: String, val attrKey: String, val scope: HttpScope, val httpHook: HttpResponseHook, val provider: AbstractCaptureProvider)
  extends AbstractCapture {
  def getScope = scope
  def getAttrKey = attrKey
  def getHttpHook = httpHook

  def capture(from: Any): Option[Any] = {
    provider.capture(expression, from)
  }
}
