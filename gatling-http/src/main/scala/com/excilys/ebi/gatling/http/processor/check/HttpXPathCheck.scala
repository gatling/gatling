package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture

class HttpXPathCheck(expressionFormatter: Context => String, expected: String, attrKey: String, httpPhase: HttpPhase, checkType: CheckType)
    extends HttpXPathCapture(expressionFormatter, attrKey, httpPhase) with HttpCheck {

  def getCheckType = checkType

  def getExpected = expected

  override def toString = "HttpXPathCheck"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathCheck])
      false
    else {
      val other = that.asInstanceOf[HttpXPathCheck]
      this.checkType == other.getCheckType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getCheckType.hashCode
}