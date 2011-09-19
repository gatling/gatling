package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

class HttpHeaderPresentAssertion(headerName: String, attrKey: String) extends HttpHeaderAssertion(headerName: String, "", attrKey: String) {
  override def getAssertionType = EXISTENCE

  override def toString = "HttpHeaderPresentAssertion (Header " + expression + " must be present')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpHeaderPresentAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpHeaderPresentAssertion]

      this.expression == other.expression && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }
}