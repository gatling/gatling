package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider
import com.excilys.ebi.gatling.http.phase.StatusReceived

import com.ning.http.client.Response

class HttpStatusCapture(attrKey: String)
    extends HttpCapture("", attrKey, new StatusReceived, HTTP_STATUS_PROVIDER) {

  override def toString = "HttpStatusCapture"
}