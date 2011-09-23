package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture

import org.apache.commons.lang3.StringUtils

class HttpRegExpAssertion(expressionFormatter: Context => String, val expected: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpRegExpCapture(expressionFormatter, attrKey, httpPhase) with HttpAssertion {

  def getAssertionType = expected match {
    case StringUtils.EMPTY => EXISTENCE
    case _ => EQUALITY
  }

  def getExpected = expected

  override def toString = getAssertionType match {
    case EXISTENCE => "HttpRegExpPresentAssertion ('" + expressionFormatter + "' must be present in response)"
    case EQUALITY => "HttpRegExpAssertion ('" + expressionFormatter + "' must be equal to '" + expected + "')"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpRegExpAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpRegExpAssertion]

      this.getAssertionType == other.getAssertionType && this.expressionFormatter == other.expressionFormatter && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getAssertionType.hashCode + this.attrKey.size
}