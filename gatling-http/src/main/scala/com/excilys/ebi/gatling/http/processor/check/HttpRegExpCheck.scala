package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture

import org.apache.commons.lang3.StringUtils

class HttpRegExpCheck(expressionFormatter: Context => String, val expected: String, attrKey: String, httpPhase: HttpPhase, checkType: CheckType)
    extends HttpRegExpCapture(expressionFormatter, attrKey, httpPhase) with HttpCheck {

  def getCheckType = checkType

  def getExpected = expected

  override def toString = getCheckType match {
    case EXISTENCE => "HttpRegExpPresentCheck ('" + expressionFormatter + "' must be present in response)"
    case EQUALITY => "HttpRegExpCheck ('" + expressionFormatter + "' must be equal to '" + expected + "')"
    case INEQUALITY => "HttpRegExpCheck ('" + expressionFormatter + "' must NOT be equal to '" + expected + "')"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpRegExpCheck])
      false
    else {
      val other = that.asInstanceOf[HttpRegExpCheck]

      this.checkType == other.getCheckType && this.expressionFormatter == other.expressionFormatter && this.expected == other.expected && this.attrKey == other.attrKey
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.checkType.hashCode + this.attrKey.size
}