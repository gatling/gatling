package com.excilys.ebi.gatling.core.provider.assertion

import com.excilys.ebi.gatling.core.log.Logging

abstract class AbstractAssertionProvider extends Logging {
  def assert(expected: String, target: Any, from: Any): (Boolean, Option[Any])
}