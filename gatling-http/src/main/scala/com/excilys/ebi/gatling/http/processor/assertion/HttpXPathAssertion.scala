package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.AssertionType._

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture

import org.apache.commons.lang3.StringUtils

class HttpXPathAssertion(expressionFormatter: Context => String, expected: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpXPathCapture(expressionFormatter, attrKey, httpPhase) with HttpAssertion {

  def getAssertionType = expected match {
    case StringUtils.EMPTY => EXISTENCE
    case _ => EQUALITY
  }

  def getExpected = expected

  override def toString = getAssertionType match {
    case EQUALITY => "HttpXPathAssertion ('" + expressionFormatter + "' must be equal to '" + expected + "')"
    case EXISTENCE => "HttpXPathPresentAssertion ('" + expressionFormatter + "' must be present)"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpXPathAssertion]
      this.getAssertionType == other.getAssertionType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getAssertionType.hashCode
}