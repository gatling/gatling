package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder

abstract class HttpAssertionBuilder[B <: HttpAssertionBuilder[B]](expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
    extends HttpProcessorBuilder {

  def newInstance(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]): B

  def in(attrKey: String): B = {
    newInstance(expression, expected, Some(attrKey), httpPhase)
  }

  override def build: HttpAssertion
}