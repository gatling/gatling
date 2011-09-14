package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider
import com.excilys.ebi.gatling.http.phase.StatusReceived

import com.ning.http.client.Response

class HttpStatusCapture(attrKey: String)
    extends HttpCapture("", attrKey, new StatusReceived, new HttpStatusCaptureProvider) {

  def captureInRequest(from: Any, identifier: String): Option[Any] = {
    provider.capture(expression, from)
  }

}