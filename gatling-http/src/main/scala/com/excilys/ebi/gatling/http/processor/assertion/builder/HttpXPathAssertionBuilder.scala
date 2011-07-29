package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.phase.CompletePageReceived
import com.excilys.ebi.gatling.http.phase.HeadersReceived
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpXPathAssertion

object HttpXPathAssertionBuilder {
  class HttpXPathAssertionBuilder(val expression: Option[String], val expected: Option[Any], val attrKey: Option[String], httpPhase: Option[HttpPhase]) {
    def in(attrKey: String) = new HttpXPathAssertionBuilder(expression, expected, Some(attrKey), httpPhase)
    def onHeaders = new HttpXPathAssertionBuilder(expression, expected, attrKey, Some(new HeadersReceived))
    def onComplete = new HttpXPathAssertionBuilder(expression, expected, attrKey, Some(new CompletePageReceived))

    def build: HttpAssertion = new HttpXPathAssertion(expression.get, expected.get, attrKey, httpPhase.get)
  }

  def assertXpath(expression: String, expected: Any) = new HttpXPathAssertionBuilder(Some(expression), Some(expected), None, Some(new CompletePageReceived))
}