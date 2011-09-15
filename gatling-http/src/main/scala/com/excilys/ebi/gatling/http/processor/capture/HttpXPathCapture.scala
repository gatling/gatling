package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.phase.HttpPhase

import com.ning.http.client.Response

class HttpXPathCapture(expression: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpCapture(expression, attrKey, httpPhase, XPATH_PROVIDER) {

  override def toString = "HttpXPathCapture (" + expression + ")"

}