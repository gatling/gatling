package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture

class HttpXPathAssertion(expression: String, val expected: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpXPathCapture(expression, attrKey, httpPhase) with HttpAssertion {

  def getAssertionType = EQUALITY

  def getExpected = expected

  override def toString = "HttpXPathAssertion ('" + expression + "' must be equal to '" + expected + "')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpXPathAssertion]
      this.expression == other.expression && this.expected == other.expected
    }
  }

  override def hashCode = this.expression.size + this.expected.size
}