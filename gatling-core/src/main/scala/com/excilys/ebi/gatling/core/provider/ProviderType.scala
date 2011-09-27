package com.excilys.ebi.gatling.core.provider

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.log.Logging

trait ProviderType extends Logging {
  def getProvider(placeToSearch: Any): AbstractCaptureProvider
}