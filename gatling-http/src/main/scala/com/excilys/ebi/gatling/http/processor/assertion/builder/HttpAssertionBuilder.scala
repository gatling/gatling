package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.phase.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder

abstract class HttpAssertionBuilder(val expression: Option[String], val attrKey: Option[String], val expected: Option[Any], httpPhase: Option[HttpPhase])
    extends HttpProcessorBuilder {

  def in(attrKey: String): HttpAssertionBuilder

  override def build: HttpAssertion
}