package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

class HttpStatusCapture(attrKey: String)
    extends HttpCapture((c: Context) => "", attrKey, StatusReceived, HTTP_STATUS_PROVIDER) {

  override def toString = "HttpStatusCapture"
}