package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

class HttpRegExpPresentAssertion(expression: String, attrKey: String, httpPhase: HttpPhase) extends HttpRegExpAssertion(expression, "", attrKey, httpPhase) {

  override def getAssertionType = EXISTENCE

  override def toString = "HttpRegExpPresentAssertion ('" + expression + "' must be present in response)"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpRegExpPresentAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpRegExpPresentAssertion]

      this.expression == other.expression && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }
}