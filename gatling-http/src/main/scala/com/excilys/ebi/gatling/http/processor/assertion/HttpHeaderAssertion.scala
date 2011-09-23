package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

class HttpHeaderAssertion(headerName: String, expected: String, attrKey: String)
    extends HttpHeaderCapture(headerName, attrKey) with HttpAssertion {

  def getAssertionType = expected match {
    case StringUtils.EMPTY => EXISTENCE
    case _ => EQUALITY
  }

  def getExpected = expected

  override def toString = getAssertionType match {
    case EXISTENCE => "HttpHeaderPresentAssertion (Header " + expression + " must be present')"
    case EQUALITY => "HttpHeaderAssertion (Header " + expression + "'s value must be equal to '" + expected + "')"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpHeaderAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpHeaderAssertion]

      this.getAssertionType == other.getAssertionType && this.expression == other.expression && this.expected == other.getExpected && this.attrKey == other.attrKey
    }
  }

  override def hashCode = this.expression.size + this.expected.size + this.getAssertionType.hashCode + this.attrKey.size
}