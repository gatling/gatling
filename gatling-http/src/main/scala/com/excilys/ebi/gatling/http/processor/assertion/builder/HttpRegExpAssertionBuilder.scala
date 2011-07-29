package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.phase.CompletePageReceived
import com.excilys.ebi.gatling.http.phase.HeadersReceived
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpRegExpAssertion

object HttpRegExpAssertionBuilder {
  class HttpRegExpAssertionBuilder(val expression: Option[String], val attrKey: Option[String], val expected: Option[Any], httpPhase: Option[HttpPhase]) {
    def in(attrKey: String) = new HttpRegExpAssertionBuilder(expression, Some(attrKey), expected, httpPhase)
    def onHeaders = new HttpRegExpAssertionBuilder(expression, attrKey, expected, Some(new HeadersReceived))
    def onComplete = new HttpRegExpAssertionBuilder(expression, attrKey, expected, Some(new CompletePageReceived))

    def build: HttpAssertion = new HttpRegExpAssertion(expression.get, attrKey, expected.get, httpPhase.get)
  }

  def assertRegexp(expression: String, expected: Any) = new HttpRegExpAssertionBuilder(Some(expression), None, Some(expected), Some(new CompletePageReceived))
}