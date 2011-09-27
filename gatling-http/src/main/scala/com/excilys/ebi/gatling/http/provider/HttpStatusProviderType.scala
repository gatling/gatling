package com.excilys.ebi.gatling.http.provider

import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider

object HttpStatusProviderType extends ProviderType {
  def getProvider(statusCode: Any) = {
    logger.debug("Instantiation of HttpStatusCaptureProvider")
    new HttpStatusCaptureProvider(statusCode.asInstanceOf[Int])
  }
}