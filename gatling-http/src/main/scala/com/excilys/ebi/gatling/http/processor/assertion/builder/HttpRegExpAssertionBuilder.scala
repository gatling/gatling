package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpRegExpAssertion

import org.apache.commons.lang3.StringUtils

object HttpRegExpAssertionBuilder {
  class HttpRegExpAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpAssertionBuilder[HttpRegExpAssertionBuilder](expression, expected, attrKey, httpPhase) {

    def newInstance(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpRegExpAssertionBuilder(expression, expected, attrKey, httpPhase)
    }

    def build: HttpAssertion =
      new HttpRegExpAssertion(expression.get, expected.get, attrKey.get, httpPhase.get)
  }

  def assertRegexp(expression: String, expected: String) = new HttpRegExpAssertionBuilder(Some(expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def assertRegexp(expression: String) = new HttpRegExpAssertionBuilder(Some(expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}