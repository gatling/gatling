package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.RegExpAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpRegExpAssertion(expression: String, expected: String, attrKey: Option[String], httpPhase: HttpPhase)
    extends HttpAssertion(expression, expected, attrKey, httpPhase, new RegExpAssertionProvider) {

  override def toString = "HttpRegExpAssertion ('" + expression + "' must be equal to '" + expected + "')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpRegExpAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpRegExpAssertion]

      this.expression == other.expression && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }
}