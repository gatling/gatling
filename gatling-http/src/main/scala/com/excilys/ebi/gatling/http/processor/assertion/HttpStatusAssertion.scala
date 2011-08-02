package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.RangeAssertionProvider

import com.excilys.ebi.gatling.http.phase.StatusReceived

class HttpStatusAssertion(expected: String, attrKey: Option[String])
    extends HttpAssertion("", expected, attrKey, new StatusReceived, new RangeAssertionProvider) {

  override def toString = "HttpStatusAssertion (Http Response Status must be in '{" + expected + "}')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpStatusAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpStatusAssertion]

      other.expected.contains(this.expected) && this.attrKey == other.attrKey
    }
  }
}