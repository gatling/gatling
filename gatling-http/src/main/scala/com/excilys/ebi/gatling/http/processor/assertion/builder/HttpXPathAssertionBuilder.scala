package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpXPathAssertion

import org.apache.commons.lang3.StringUtils

object HttpXPathAssertionBuilder {
  class HttpXPathAssertionBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpAssertionBuilder[HttpXPathAssertionBuilder](expressionFormatter, expected, attrKey, httpPhase) {

    def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpXPathAssertionBuilder(expressionFormatter, expected, attrKey, httpPhase)
    }

    def build = new HttpXPathAssertion(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get)
  }

  def assertXpath(expression: String, expected: String) = new HttpXPathAssertionBuilder(Some((c: Context) => expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def assertXpath(expression: String) = new HttpXPathAssertionBuilder(Some((c: Context) => expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}