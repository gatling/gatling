package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture

class HttpRegExpAssertion(expression: String, val expected: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpRegExpCapture(expression, attrKey, httpPhase) with HttpAssertion {

  def getAssertionType = EQUALITY

  def getExpected = expected

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