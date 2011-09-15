package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.phase.HeadersReceived

class HttpHeaderCapture(expression: String, attrKey: String)
    extends HttpCapture(expression, attrKey, new HeadersReceived, HTTP_HEADERS_PROVIDER) {

  override def toString = "HttpHeaderCapture (" + expression + ")"
}