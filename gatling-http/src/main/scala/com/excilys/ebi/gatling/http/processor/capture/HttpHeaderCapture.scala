package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.HttpHeaderProviderType

class HttpHeaderCapture(expression: Context => String, attrKey: String)
    extends HttpCapture(expression, attrKey, HeadersReceived, HttpHeaderProviderType) {

  override def toString = "HttpHeaderCapture"
}