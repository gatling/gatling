package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.request.HttpPhase._

class HttpXPathPresentAssertion(expression: String, attrKey: String, httpPhase: HttpPhase) extends HttpXPathAssertion(expression, "", attrKey, httpPhase) {
  override def getAssertionType = EXISTENCE

  override def toString = "HttpXPathPresentAssertion ('" + expression + "' must be present)"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathPresentAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpXPathPresentAssertion]
      this.expression == other.expression && this.expected == other.expected
    }
  }

  override def hashCode = this.expression.size + this.expected.size
}