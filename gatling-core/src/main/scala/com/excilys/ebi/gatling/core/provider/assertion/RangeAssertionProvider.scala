package com.excilys.ebi.gatling.core.provider.assertion

class RangeAssertionProvider extends AbstractAssertionProvider {

  def assert(expected: String, target: Any, from: Any) = (expected.contains(from.toString), Option(from.toString))

}