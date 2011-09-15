package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

import scala.util.matching.Regex

import com.ning.http.client.Response

class HttpRegExpCapture(expression: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpCapture(expression, attrKey, httpPhase, REGEXP_PROVIDER) {

  override def toString = "HttpRegExpCapture (" + expression + ")"

}