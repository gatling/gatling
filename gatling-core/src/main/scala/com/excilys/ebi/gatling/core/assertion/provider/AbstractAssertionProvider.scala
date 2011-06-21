package com.excilys.ebi.gatling.core.assertion.provider

abstract class AbstractAssertionProvider {
  def assert(expected: Any, actual: Any)
}