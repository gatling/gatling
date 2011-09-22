package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

class HttpXPathCapture(expression: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpCapture(expression, attrKey, httpPhase, XPATH_PROVIDER) {

  override def toString = "HttpXPathCapture (" + expression + ")"

}