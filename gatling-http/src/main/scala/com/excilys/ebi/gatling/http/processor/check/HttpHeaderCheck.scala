package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.processor.capture.HttpHeaderCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

import org.apache.commons.lang3.StringUtils

class HttpHeaderCheck(headerNameFormatter: Context => String, expected: String, attrKey: String, checkType: CheckType)
    extends HttpHeaderCapture(headerNameFormatter, attrKey) with HttpCheck {

  def getCheckType = checkType

  def getExpected = expected

  override def toString = checkType match {
    case EXISTENCE => "HttpHeaderPresentCheck (Header " + expressionFormatter + " must be present')"
    case INEXISTENCE => "HttpHeaderPresentCheck (Header " + expressionFormatter + " must NOT be present')"
    case EQUALITY => "HttpHeaderCheck (Header " + expressionFormatter + "'s value must be equal to '" + expected + "')"
    case INEQUALITY => "HttpHeaderCheck (Header " + expressionFormatter + "'s value must NOT be equal to '" + expected + "')"
  }

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpHeaderCheck])
      false
    else {
      val other = that.asInstanceOf[HttpHeaderCheck]

      this.checkType == other.getCheckType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected && this.attrKey == other.attrKey
    }
  }

  override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getCheckType.hashCode + this.attrKey.size + this.checkType.hashCode
}