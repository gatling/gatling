package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider
import com.excilys.ebi.gatling.http.processor.capture.HttpStatusCapture

class HttpStatusAssertion(val expected: String, attrKey: String)
    extends HttpStatusCapture(attrKey) with HttpAssertion {

  def getAssertionType = IN_RANGE

  def getExpected = expected

  override def toString = "HttpStatusAssertion (Http Response Status must be in '{" + expected + "}')"

  override def equals(that: Any) = that.isInstanceOf[HttpStatusAssertion]

  override def hashCode() = 1
}