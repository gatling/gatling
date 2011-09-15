package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.phase.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpXPathAssertion
import org.apache.commons.lang3.StringUtils

object HttpXPathAssertionBuilder {
  class HttpXPathAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpAssertionBuilder(expression, expected, attrKey, httpPhase) {
    def in(attrKey: String) = new HttpXPathAssertionBuilder(expression, expected, Some(attrKey), httpPhase)

    def build: HttpAssertion = new HttpXPathAssertion(expression.get, expected.get, attrKey.get, httpPhase.get)
  }

  def assertXpath(expression: String, expected: String) = new HttpXPathAssertionBuilder(Some(expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}