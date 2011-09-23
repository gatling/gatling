package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpHeaderAssertion
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

object HttpHeaderAssertionBuilder {
  class HttpHeaderAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String])
      extends HttpAssertionBuilder[HttpHeaderAssertionBuilder](expression, expected, attrKey, Some(HeadersReceived)) {

    def newInstance(expression: Option[String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase]) = {
      new HttpHeaderAssertionBuilder(expression, expected, attrKey)
    }

    def build: HttpAssertion =
      new HttpHeaderAssertion(expression.get, expected.get, attrKey.get)
  }

  def assertHeader(headerName: String, expected: String) = new HttpHeaderAssertionBuilder(Some(headerName), Some(expected), Some(StringUtils.EMPTY))
  def assertHeader(headerName: String) = new HttpHeaderAssertionBuilder(Some(headerName), Some(StringUtils.EMPTY), Some(StringUtils.EMPTY))
}