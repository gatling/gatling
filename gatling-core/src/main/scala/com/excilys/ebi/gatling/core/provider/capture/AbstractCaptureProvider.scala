package com.excilys.ebi.gatling.core.provider.capture

import com.excilys.ebi.gatling.core.log.Logging

abstract class AbstractCaptureProvider(placeToSearch: Any) extends Logging {
  def capture(expression: Any): Option[String]
}