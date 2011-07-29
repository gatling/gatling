package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.XPathAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpXPathAssertion(expression: String, expected: Any, attrKey: Option[String], httpPhase: HttpPhase)
    extends HttpAssertion(expression, expected, attrKey, httpPhase, new XPathAssertionProvider) {

  override def toString = "HttpXPathAssertion ('" + expression + "' must be equal to '" + expected + "')"
}