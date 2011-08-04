package com.excilys.ebi.gatling.http.processor.assertion
import com.excilys.ebi.gatling.http.phase.HeadersReceived
import com.excilys.ebi.gatling.http.provider.assertion.HttpHeadersAssertionProvider

class HttpHeaderAssertion(headerName: String, expected: String, attrKey: Option[String])
    extends HttpAssertion(headerName, expected, attrKey, new HeadersReceived, new HttpHeadersAssertionProvider) {

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