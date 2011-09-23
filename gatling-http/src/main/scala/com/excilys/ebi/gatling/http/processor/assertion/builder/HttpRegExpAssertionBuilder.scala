package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpRegExpAssertion

import org.apache.commons.lang3.StringUtils

object HttpRegExpAssertionBuilder {
  class HttpRegExpAssertionBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpAssertionBuilder[HttpRegExpAssertionBuilder](expressionFormatter, expected, attrKey, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpRegExpAssertionBuilder(expressionFormatter, expected, attrKey, httpPhase)
    }

    def build: HttpAssertion =
      new HttpRegExpAssertion(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get)
  }

  def assertRegexp(expression: String, expected: String) = new HttpRegExpAssertionBuilder(Some((c: Context) => expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def assertRegexp(expression: String) = new HttpRegExpAssertionBuilder(Some((c: Context) => expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}