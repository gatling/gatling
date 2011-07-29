package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.RegExpAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpRegExpAssertion(expression: String, attrKey: Option[String], expected: Any, httpPhase: HttpPhase)
    extends HttpAssertion(expression, expected, attrKey, httpPhase, new RegExpAssertionProvider) {

  override def toString = "HttpRegExpAssertion ('" + expression + "' must be equal to '" + expected + "')"
}