package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpXPathAssertion

import org.apache.commons.lang3.StringUtils

object HttpXPathAssertionBuilder {
  class HttpXPathAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase])
      extends HttpAssertionBuilder[HttpXPathAssertionBuilder](expression, expected, attrKey, httpPhase) {

    def newInstance(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpXPathAssertionBuilder(expression, expected, attrKey, httpPhase)
    }

    def build = new HttpXPathAssertion(expression.get, expected.get, attrKey.get, httpPhase.get)
  }

  def assertXpath(expression: String, expected: String) = new HttpXPathAssertionBuilder(Some(expression), Some(expected), Some(StringUtils.EMPTY), Some(CompletePageReceived))
  def assertXpath(expression: String) = new HttpXPathAssertionBuilder(Some(expression), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY), Some(CompletePageReceived))
}