package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.phase.HeadersReceived
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider
import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture

class HttpHeaderAssertion(headerName: String, val expected: String, attrKey: String)
    extends HttpHeaderCapture(headerName, attrKey) with HttpAssertion {

  def getAssertionType = EQUALITY

  def getExpected = expected

  override def toString = "HttpHeaderAssertion (Header " + expression + "'s value must be equal to '" + expected + "')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpHeaderAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpHeaderAssertion]

      this.expression == other.expression && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }
}