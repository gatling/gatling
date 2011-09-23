package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

class HttpHeaderCapture(expression: Context => String, attrKey: String)
    extends HttpCapture(expression, attrKey, HeadersReceived, HTTP_HEADERS_PROVIDER) {

  override def toString = "HttpHeaderCapture (" + expression + ")"
}