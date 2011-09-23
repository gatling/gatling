package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

class HttpHeaderAssertion(headerNameFormatter: Context => String, expected: String, attrKey: String)
    extends HttpHeaderCapture(headerNameFormatter, attrKey) with HttpAssertion {

  def getAssertionType = expected match {
    case StringUtils.EMPTY => EXISTENCE
    case _ => EQUALITY
  }

  def getExpected = expected

  override def toString = getAssertionType match {
    case EXISTENCE => "HttpHeaderPresentAssertion (Header " + expressionFormatter + " must be present')"
    case EQUALITY => "HttpHeaderAssertion (Header " + expressionFormatter + "'s value must be equal to '" + expected + "')"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpHeaderAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpHeaderAssertion]

      this.getAssertionType == other.getAssertionType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected && this.attrKey == other.attrKey
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getAssertionType.hashCode + this.attrKey.size
}