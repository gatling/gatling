package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.XPathAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpXPathAssertion(expression: String, expected: String, attrKey: Option[String], httpPhase: HttpPhase)
    extends HttpAssertion(expression, expected, attrKey, httpPhase, new XPathAssertionProvider) {

  override def toString = "HttpXPathAssertion ('" + expression + "' must be equal to '" + expected + "')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpXPathAssertion]

      this.expression == other.expression && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }
}