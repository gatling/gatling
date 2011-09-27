package com.excilys.ebi.gatling.http.provider

import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

import com.ning.http.client.Response

object HttpRegExpProviderType extends ProviderType {
  def getProvider(textContent: Any) = {
    logger.debug("Instantiation of RegExpCaptureProvider")
    new RegExpCaptureProvider(textContent.asInstanceOf[Response].getResponseBody)
  }
}