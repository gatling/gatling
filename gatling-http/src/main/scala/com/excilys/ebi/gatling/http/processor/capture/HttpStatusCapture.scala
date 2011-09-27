package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.HttpStatusProviderType

class HttpStatusCapture(attrKey: String)
    extends HttpCapture((c: Context) => "", attrKey, StatusReceived, HttpStatusProviderType) {

  override def toString = "HttpStatusCapture"
}