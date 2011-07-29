package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

abstract class HttpAssertionBuilder(val expression: String, val attrKey: Option[String], val expected: Any, httpPhase: HttpPhase, val provider: AbstractAssertionProvider) {
  def in(attrKey: String): HttpAssertionBuilder
}