package com.excilys.ebi.gatling.http.provider

import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

import com.ning.http.client.Response

object HttpXPathProviderType extends ProviderType {
  def getProvider(xmlContent: Any) = {
    logger.debug("Instantiation of XPathCaptureProvider")
    new XPathCaptureProvider(xmlContent.asInstanceOf[Response].getResponseBodyAsBytes)
  }
}