package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.ProviderType._

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpPhase._

abstract class HttpCapture(val expression: String, val attrKey: String, httpPhase: HttpPhase, val providerType: ProviderType)
    extends HttpProcessor(httpPhase) {

  def getAttrKey = attrKey

  def getProviderType = providerType
}
