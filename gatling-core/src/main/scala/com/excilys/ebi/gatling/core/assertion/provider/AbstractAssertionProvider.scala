package com.excilys.ebi.gatling.core.assertion.provider

import com.excilys.ebi.gatling.core.log.Logging

abstract class AbstractAssertionProvider extends Logging {
  def assert(expected: Any, actual: Any)
}