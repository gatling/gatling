package com.excilys.ebi.gatling.core.assertion.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object RegExpAssertionProvider {
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[RegExpAssertionProvider]);
}
class RegExpAssertionProvider extends AbstractAssertionProvider {
  def assert(expected: Any, actual: Any) = {}
}
