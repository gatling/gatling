package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture

import org.apache.commons.lang3.StringUtils

class HttpXPathCheck(expressionFormatter: Context => String, expected: String, attrKey: String, httpPhase: HttpPhase)
    extends HttpXPathCapture(expressionFormatter, attrKey, httpPhase) with HttpCheck {

  def getCheckType = expected match {
    case StringUtils.EMPTY => EXISTENCE
    case _ => EQUALITY
  }

  def getExpected = expected

  override def toString = getCheckType match {
    case EQUALITY => "HttpXPathCheck ('" + expressionFormatter + "' must be equal to '" + expected + "')"
    case EXISTENCE => "HttpXPathPresentCheck ('" + expressionFormatter + "' must be present)"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathCheck])
      false
    else {
      val other = that.asInstanceOf[HttpXPathCheck]
      this.getCheckType == other.getCheckType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getCheckType.hashCode
}