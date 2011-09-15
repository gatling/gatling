package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase._

abstract class HttpCapture(val expression: String, val attrKey: String, httpPhase: HttpPhase, val providerType: ProviderType)
    extends HttpProcessor(httpPhase) {

  def getAttrKey = attrKey

  def getProviderType = providerType
}
