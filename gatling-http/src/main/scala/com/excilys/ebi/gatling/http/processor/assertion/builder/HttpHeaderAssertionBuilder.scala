package com.excilys.ebi.gatling.http.processor.assertion.builder

import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpHeaderAssertion
import com.excilys.ebi.gatling.http.header.HeaderKey

object HttpHeaderAssertionBuilder {
  class HttpHeaderAssertionBuilder(expression: Option[String], expected: Option[String], attrKey: Option[String])
      extends HttpAssertionBuilder(expression, expected, attrKey, null) {
    def in(attrKey: String) = new HttpHeaderAssertionBuilder(expression, expected, Some(attrKey))

    def build: HttpAssertion = new HttpHeaderAssertion(expression.get, expected.get, attrKey)
  }

  def assertHeader(headerName: String, expected: String) = new HttpHeaderAssertionBuilder(Some(headerName), Some(expected), None)
  def assertHeader(header: HeaderKey, expected: String): HttpHeaderAssertionBuilder = assertHeader(header.toString, expected)
}