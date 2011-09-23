package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider
import com.excilys.ebi.gatling.http.processor.capture.HttpStatusCapture

class HttpStatusCheck(val expected: String, attrKey: String)
    extends HttpStatusCapture(attrKey) with HttpCheck {

  def getCheckType = IN_RANGE

  def getExpected = expected

  override def toString = "HttpStatusCheck (Http Response Status must be in '{" + expected + "}')"

  override def equals(that: Any) = that.isInstanceOf[HttpStatusCheck]

  override def hashCode() = 1
}