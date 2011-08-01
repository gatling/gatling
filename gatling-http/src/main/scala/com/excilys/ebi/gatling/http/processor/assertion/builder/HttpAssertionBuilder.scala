package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

abstract class HttpAssertionBuilder(val expression: Option[String], val attrKey: Option[String], val expected: Option[Any], httpPhase: Option[HttpPhase]) {
  def in(attrKey: String): HttpAssertionBuilder
}