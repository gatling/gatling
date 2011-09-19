package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpHeaderAssertion
import org.apache.commons.lang3.StringUtils
import com.excilys.ebi.gatling.http.processor.assertion.HttpHeaderPresentAssertion

object HttpHeaderAssertionBuilder {
  class HttpHeaderAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String])
      extends HttpAssertionBuilder(expression, expected, attrKey, null) {
    def in(attrKey: String) = new HttpHeaderAssertionBuilder(expression, expected, Some(attrKey))

    def build: HttpAssertion =
      if (expected.isEmpty)
        new HttpHeaderPresentAssertion(expression.get, attrKey.get)
      else
        new HttpHeaderAssertion(expression.get, expected.get, attrKey.get)
  }

  def assertHeader(headerName: String, expected: String) = new HttpHeaderAssertionBuilder(Some(headerName), Some(expected), Some(StringUtils.EMPTY))
  def assertHeader(headerName: String) = new HttpHeaderAssertionBuilder(Some(headerName), None, Some(StringUtils.EMPTY))
}